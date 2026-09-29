package net.tearpelato.craftcorelib.platform;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.api.config.ConfigValue;
import net.tearpelato.craftcorelib.api.config.util.ConfigBinder;
import net.tearpelato.craftcorelib.platform.services.IConfigHelper;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class FabricConfigHelper implements IConfigHelper {

    private static final Map<String, Map<ConfigType, Map<String, Object>>> RUNTIME_VALUES = new HashMap<>();
    private static final Map<String, Map<ConfigType, List<ConfigCategory>>> CATEGORIES = new HashMap<>();

    @Override
    public void register(String modId, List<ConfigCategory> categories) {
        Map<ConfigType, List<ConfigCategory>> byType = net.tearpelato.craftcorelib.api.config.util.ConfigBinder.groupByType(categories);
        CATEGORIES.computeIfAbsent(modId, key -> new EnumMap<>(ConfigType.class)).putAll(byType);

        for (Map.Entry<ConfigType, List<ConfigCategory>> entry : byType.entrySet()) {
            ConfigType type = entry.getKey();

            if (type == ConfigType.CLIENT && FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
                continue;
            }

            registerType(modId, type, entry.getValue());
        }
    }

    @Override
    public void save(String modId, ConfigType type) {
        saveType(modId, type);
    }

    @Override
    public Map<String, Function<Screen, Screen>> getExternalConfigScreens() {
        Map<String, Function<Screen, Screen>> result = new LinkedHashMap<>();
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) return result;

        Screen probe = Minecraft.getInstance().screen;
        for (EntrypointContainer<ModMenuApi> c :
                FabricLoader.getInstance().getEntrypointContainers("modmenu", ModMenuApi.class)) {
            try {
                ModMenuApi api = c.getEntrypoint();
                String id = c.getProvider().getMetadata().getId();

                addIfUseful(result, id, api.getModConfigScreenFactory(), probe);
                api.getProvidedConfigScreenFactories()
                        .forEach((modId, f) -> addIfUseful(result, modId, f, probe));
            } catch (Throwable ignored) {}
        }
        return result;
    }

    private static void addIfUseful(Map<String, Function<Screen, Screen>> map, String id,
                                    ConfigScreenFactory<?> f, Screen probe) {
        if (f != null && f.create(probe) != null) {
            map.put(id, f::create);
        }
    }

    private void registerType(String modId, ConfigType type, List<ConfigCategory> categories) {
        Map<String, Object> values = RUNTIME_VALUES
                .computeIfAbsent(modId, key -> new EnumMap<>(ConfigType.class))
                .computeIfAbsent(type, key -> new HashMap<>());

        Path configPath = resolvePath(modId, type);

        try (CommentedFileConfig fileConfig = openFile(configPath)) {
            fileConfig.load();

            for (ConfigCategory category : categories) {
                if (category.getCommentKey() != null) {
                    fileConfig.setComment(category.getName(), category.getCommentKey());
                }

                for (ConfigValue<?> value : category.getValues()) {
                    String fullKey = net.tearpelato.craftcorelib.api.config.util.ConfigBinder.fullKey(category, value);
                    Object def = value.getDefault();

                    Object current = fileConfig.contains(fullKey) ? fileConfig.get(fullKey) : def;
                    current = net.tearpelato.craftcorelib.api.config.util.ConfigBinder.clampRaw(current, value);

                    values.put(fullKey, current);
                    fileConfig.set(fullKey, current);

                    if (value.getCommentKey() != null) {
                        fileConfig.setComment(fullKey, value.getCommentKey());
                    }
                }
            }

            fileConfig.save();
        }

        net.tearpelato.craftcorelib.api.config.util.ConfigBinder.bindAll(categories, new net.tearpelato.craftcorelib.api.config.util.ConfigBackend() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(ConfigCategory category, ConfigValue<T> value) {
                Object stored = values.get(net.tearpelato.craftcorelib.api.config.util.ConfigBinder.fullKey(category, value));
                return stored != null ? (T) stored : value.getDefault();
            }

            @Override
            public <T> void set(ConfigCategory category, ConfigValue<T> value, T newValue) {
                T clamped = net.tearpelato.craftcorelib.api.config.util.ConfigBinder.clamp(newValue, value);
                values.put(net.tearpelato.craftcorelib.api.config.util.ConfigBinder.fullKey(category, value), clamped);
            }
        });
    }

    private static void saveType(String modId, ConfigType type) {
        Map<ConfigType, List<ConfigCategory>> byType = CATEGORIES.get(modId);
        Map<ConfigType, Map<String, Object>> byModValues = RUNTIME_VALUES.get(modId);
        if (byType == null || byModValues == null) {
            return;
        }

        List<ConfigCategory> categories = byType.get(type);
        Map<String, Object> values = byModValues.get(type);
        if (categories == null || values == null) {
            return;
        }

        try (CommentedFileConfig fileConfig = openFile(resolvePath(modId, type))) {
            fileConfig.load();

            for (ConfigCategory category : categories) {
                if (category.getCommentKey() != null) {
                    fileConfig.setComment(category.getName(), category.getCommentKey());
                }

                for (ConfigValue<?> value : category.getValues()) {
                    String fullKey = ConfigBinder.fullKey(category, value);
                    Object stored = values.get(fullKey);
                    if (stored != null) {
                        fileConfig.set(fullKey, stored);
                    }
                    if (value.getCommentKey() != null) {
                        fileConfig.setComment(fullKey, value.getCommentKey());
                    }
                }
            }

            fileConfig.save();
        }
    }

    private static Path resolvePath(String modId, ConfigType type) {
        String suffix = switch (type) {
            case CLIENT -> "-client";
            case SERVER -> "-server";
            case COMMON -> "";
        };
        return FabricLoader.getInstance().getConfigDir().resolve(modId + suffix + ".toml");
    }

    private static CommentedFileConfig openFile(Path path) {
        return CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();
    }
}
