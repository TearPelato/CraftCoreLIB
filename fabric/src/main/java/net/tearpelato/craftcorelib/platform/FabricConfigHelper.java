package net.tearpelato.craftcorelib.platform;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.fabricmc.loader.api.FabricLoader;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigValue;
import net.tearpelato.craftcorelib.platform.services.IConfigHelper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FabricConfigHelper implements IConfigHelper {

    private static final Map<String, Map<String, Object>> RUNTIME_VALUES = new HashMap<>();
    private static final Map<String, List<ConfigCategory>> CATEGORIES = new HashMap<>();

    @Override
    public void register(String modId, List<ConfigCategory> categories) {
        CATEGORIES.put(modId, categories);

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(modId + ".toml");
        Map<String, Object> values = RUNTIME_VALUES.computeIfAbsent(modId, k -> new HashMap<>());

        try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(configPath)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {

            fileConfig.load();

            for (ConfigCategory category : categories) {
                String catName = category.getName();

                if (category.getCommentKey() != null) {
                    fileConfig.setComment(catName, category.getCommentKey());
                }

                for (ConfigValue<?> value : category.getValues()) {
                    String fullKey = catName + "." + value.getKey();
                    Object def = value.getDefault();

                    Object current = fileConfig.contains(fullKey)
                            ? fileConfig.get(fullKey)
                            : def;

                    current = clamp(current, value);

                    values.put(fullKey, current);
                    fileConfig.set(fullKey, current);

                    if (value.getCommentKey() != null) {
                        fileConfig.setComment(fullKey, value.getCommentKey());
                    }

                   bindValue(value,values,fullKey, modId);
                }
            }

            fileConfig.save();
        }
    }

    public static void saveConfig(String modId) {
        Map<String, Object> values = RUNTIME_VALUES.get(modId);
        if (values == null) return;

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(modId + ".toml");

        try (CommentedFileConfig fileConfig = CommentedFileConfig.builder(configPath)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build()) {

            fileConfig.load();

            List<ConfigCategory> cats = CATEGORIES.get(modId);
            if (cats != null) {
                for (ConfigCategory category : cats) {
                    String catName = category.getName();

                    if (category.getCommentKey() != null) {
                        fileConfig.setComment(catName, category.getCommentKey());
                    }

                    for (ConfigValue<?> value : category.getValues()) {
                        String fullKey = catName + "." + value.getKey();
                        Object val = values.get(fullKey);
                        if (val != null) {
                            fileConfig.set(fullKey, val);
                        }
                        if (value.getCommentKey() != null) {
                            fileConfig.setComment(fullKey, value.getCommentKey());
                        }
                    }
                }
            }

            fileConfig.save();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Object clamp(Object value, ConfigValue<T> configValue) {
        if (value == null) return configValue.getDefault();

        T min = configValue.getMin();
        T max = configValue.getMax();
        if (min == null && max == null) return value;

        if (value instanceof Number num && min instanceof Number minN && max instanceof Number maxN) {
            double d = num.doubleValue();
            d = Math.max(minN.doubleValue(), Math.min(maxN.doubleValue(), d));

            if (value instanceof Integer)
                return (int) d;
            if (value instanceof Long)
                return (long) d;
            if (value instanceof Float)
                return (float) d;
            if (value instanceof Double)
                return d;
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    private static <T> void bindValue(ConfigValue<T> value, Map<String, Object> values, String key, String modId) {
        value.bind(
                () -> {
                    Object v = values.get(key);
                    return v != null ? (T) v : value.getDefault();
                },
                v -> {
                    Object clamped = clamp(v, value);
                    values.put(key, clamped);
                    saveConfig(modId);
                }
        );
    }
}