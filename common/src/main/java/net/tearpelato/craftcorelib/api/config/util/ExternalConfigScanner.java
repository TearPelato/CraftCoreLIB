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
        String lowerName = fileName.toLowerCase(Locale.ROOT);

        ConfigType type;
        String baseName;

        if (lowerName.endsWith("-client.toml")) {
            type = ConfigType.CLIENT;
            baseName = fileName.substring(0, fileName.length() - "-client.toml".length());
        } else if (lowerName.endsWith("-server.toml")) {
            type = ConfigType.SERVER;
            baseName = fileName.substring(0, fileName.length() - "-server.toml".length());
        } else if (lowerName.endsWith("-common.toml")) {
            type = ConfigType.COMMON;
            baseName = fileName.substring(0, fileName.length() - "-common.toml".length());
        } else {
            type = ConfigType.COMMON;
            baseName = fileName.substring(0, fileName.length() - ".toml".length());
        }

        String extractedModId = baseName.toLowerCase(Locale.ROOT);
        int lastDash = extractedModId.lastIndexOf('-');
        if (lastDash > 0) {
            String candidate = extractedModId.substring(0, lastDash);
            if (Services.PLATFORM.isModLoaded(candidate)) {
                extractedModId = candidate;
            }
        }

        final String modId;
        if (Services.PLATFORM.isModLoaded(extractedModId)) {
            modId = extractedModId;
        } else if (Services.PLATFORM.isModLoaded(baseName.toLowerCase(Locale.ROOT))) {
            modId = baseName.toLowerCase(Locale.ROOT);
        } else {
            return;
        }

        Map<String, ConfigCategory> categories = new LinkedHashMap<>();
        Map<String, Object> runtimeValues = new LinkedHashMap<>();

        try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {

            fileConfig.load();

            for (Map.Entry<String, Object> entry : fileConfig.valueMap().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;

                if (value instanceof CommentedConfig subTable) {
                    ConfigCategory cat = categories.computeIfAbsent(key, k ->
                            ConfigCategory.create(k, type));
                    flattenTable(subTable, cat, runtimeValues, "");
                } else if (value instanceof Boolean || value instanceof Number || value instanceof String) {
                    ConfigCategory general = categories.computeIfAbsent("general", k ->
                            ConfigCategory.create("general", type));
                    ConfigValue<Object> configValue = general.define(key, value);
                    runtimeValues.put(ConfigBinder.fullKey(general, configValue), value);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not parse external config {}: {}", path.getFileName(), e.getMessage());
            return;
        }

        categories.values().removeIf(c -> c.getValues().isEmpty());
        if (categories.isEmpty()) {
            return;
        }

        List<ConfigCategory> catList = List.copyOf(categories.values());
        ConfigManager.registerExternal(modId, catList);

        final Path configPath = path;
        ConfigBinder.bindAll(catList, new ConfigBackend() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(ConfigCategory cat, ConfigValue<T> value) {
                Object stored = runtimeValues.get(ConfigBinder.fullKey(cat, value));
                return stored != null ? (T) stored : value.getDefault();
            }

            @Override
            public <T> void set(ConfigCategory cat, ConfigValue<T> value, T newValue) {
                String fullKey = ConfigBinder.fullKey(cat, value);
                runtimeValues.put(fullKey, newValue);
                try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(configPath)
                        .sync()
                        .autosave()
                        .writingMode(WritingMode.REPLACE)
                        .build()) {
                    fileConfig.load();

                    String writeKey = cat.getName() + "." + value.getKey();
                    if (value.getKey().contains(".")) {
                        writeKey = cat.getName() + "." + value.getKey();
                    }
                    fileConfig.set(writeKey, newValue);
                    fileConfig.save();
                } catch (Exception e) {
                    LOGGER.warn("Failed to write external config value {} to {}", fullKey, configPath, e);
                }
            }
        });

        LOGGER.debug("Registered external config for mod {} (file: {}, type: {}) with {} categories: {}",
                modId, fileName, type, catList.size(),
                catList.stream().map(ConfigCategory::getName).toList());
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