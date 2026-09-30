package net.tearpelato.craftcorelib.api.config.util;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.mojang.logging.LogUtils;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigManager;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.api.config.ConfigValue;
import net.tearpelato.craftcorelib.platform.Services;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExternalConfigScanner {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ExternalConfigScanner() {}

    public static void scan(Path configDir) {
        if (configDir == null || !Files.isDirectory(configDir)) {
            return;
        }

        try (var stream = Files.list(configDir)) {
            stream.filter(p -> {
                        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".toml");
                    })
                    .forEach(ExternalConfigScanner::processFile);
        } catch (IOException e) {
            LOGGER.warn("Failed to scan config directory for external configs", e);
        }
    }

    private static void processFile(Path path) {
        String fileName = path.getFileName().toString();

        String modId;
        ConfigType type;

        if (fileName.endsWith("-client.toml")) {
            modId = fileName.substring(0, fileName.length() - "-client.toml".length());
            type = ConfigType.CLIENT;
        } else if (fileName.endsWith("-server.toml")) {
            modId = fileName.substring(0, fileName.length() - "-server.toml".length());
            type = ConfigType.SERVER;
        } else if (fileName.endsWith(".toml")) {
            modId = fileName.substring(0, fileName.length() - ".toml".length());
            type = ConfigType.COMMON;
        } else {
            return;
        }

        boolean alreadyHasType = ConfigManager.getCategories(modId).stream()
                .anyMatch(c -> c.getType() == type);
        if (alreadyHasType) {
            return;
        }
        if (!Services.PLATFORM.isModLoaded(modId)) {
            return;
        }

        try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {

            fileConfig.load();

            Map<String, ConfigCategory> categories = new LinkedHashMap<>();
            Map<String, Object> values = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : fileConfig.valueMap().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;

                if (value instanceof CommentedConfig subTable) {
                    ConfigCategory cat = categories.computeIfAbsent(key, k ->
                            ConfigCategory.create(k, type)
                                    .title("config." + modId + "." + k));
                    flattenTable(subTable, cat, values, "");
                } else if (value instanceof Boolean || value instanceof Number || value instanceof String) {
                    ConfigCategory general = categories.computeIfAbsent("general", k ->
                            ConfigCategory.create("general", type)
                                    .title("config." + modId + ".general"));
                    ConfigValue<Object> configValue = general.define(key, value);
                    values.put(ConfigBinder.fullKey(general, configValue), value);
                }
            }

            categories.values().removeIf(c -> c.getValues().isEmpty());
            if (categories.isEmpty()) {
                return;
            }

            List<ConfigCategory> catList = List.copyOf(categories.values());
            for (ConfigCategory category : catList) {
                ConfigManager.register(modId, category);
            }

            ConfigBinder.bindAll(catList, new ConfigBackend() {
                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(ConfigCategory cat, ConfigValue<T> value) {
                    Object stored = values.get(ConfigBinder.fullKey(cat, value));
                    return stored != null ? (T) stored : value.getDefault();
                }

                @Override
                public <T> void set(ConfigCategory cat, ConfigValue<T> value, T newValue) {
                    String fullKey = ConfigBinder.fullKey(cat, value);
                    values.put(fullKey, newValue);

                    String writeKey = value.getKey();
                    if (!"general".equals(cat.getName())) {
                        writeKey = cat.getName() + "." + value.getKey();
                    }
                    fileConfig.set(writeKey, newValue);
                    fileConfig.save();
                }
            });

            LOGGER.debug("Registered external config for mod {} ({}) with {} categories",
                    modId, type, catList.size());
        } catch (Exception e) {
            LOGGER.debug("Could not parse external config {}: {}", path.getFileName(), e.getMessage());
        }
    }

    private static void flattenTable(CommentedConfig table, ConfigCategory category,
                                     Map<String, Object> values, String prefix) {
        for (Map.Entry<String, Object> entry : table.valueMap().entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value == null) continue;

            if (value instanceof CommentedConfig nested) {
                flattenTable(nested, category, values, key);
            } else if (value instanceof Boolean || value instanceof Number || value instanceof String) {
                ConfigValue<Object> configValue = category.define(key, value);
                values.put(ConfigBinder.fullKey(category, configValue), value);
            }
        }
    }
}