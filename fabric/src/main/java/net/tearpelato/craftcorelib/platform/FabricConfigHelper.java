package net.tearpelato.craftcorelib.platform;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.tearpelato.craftcorelib.api.config.*;
import net.tearpelato.craftcorelib.platform.services.IConfigHelper;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FabricConfigHelper implements IConfigHelper {

    private static final Map<String, Map<ConfigType, Map<String, Object>>> RUNTIME_VALUES = new HashMap<>();
    private static final Map<String, Map<ConfigType, List<ConfigCategory>>> CATEGORIES = new HashMap<>();

    @Override
    public void register(String modId, List<ConfigCategory> categories) {
        Map<ConfigType, List<ConfigCategory>> byType = ConfigBinder.groupByType(categories);
        CATEGORIES.computeIfAbsent(modId, key -> new EnumMap<>(ConfigType.class)).putAll(byType);

        for (Map.Entry<ConfigType, List<ConfigCategory>> entry : byType.entrySet()) {
            ConfigType type = entry.getKey();

            if (type == ConfigType.CLIENT && FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
                continue;
            }

            registerType(modId, type, entry.getValue());
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
                    String fullKey = ConfigBinder.fullKey(category, value);
                    Object def = value.getDefault();

                    Object current = fileConfig.contains(fullKey) ? fileConfig.get(fullKey) : def;
                    current = ConfigBinder.clampRaw(current, value);

                    values.put(fullKey, current);
                    fileConfig.set(fullKey, current);

                    if (value.getCommentKey() != null) {
                        fileConfig.setComment(fullKey, value.getCommentKey());
                    }
                }
            }

            fileConfig.save();
        }

        ConfigBinder.bindAll(categories, new ConfigBackend() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(ConfigCategory category, ConfigValue<T> value) {
                Object stored = values.get(ConfigBinder.fullKey(category, value));
                return stored != null ? (T) stored : value.getDefault();
            }

            @Override
            public <T> void set(ConfigCategory category, ConfigValue<T> value, T newValue) {
                T clamped = ConfigBinder.clamp(newValue, value);
                values.put(ConfigBinder.fullKey(category, value), clamped);
                saveType(modId, type);
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
