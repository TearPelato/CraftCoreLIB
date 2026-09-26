package net.tearpelato.craftcorelib.api.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public final class ConfigBinder {

    private ConfigBinder() {
    }

    public static void bindAll(List<ConfigCategory> categories, ConfigBackend backend) {
        for (ConfigCategory category : categories) {
            for (ConfigValue<?> value : category.getValues()) {
                bindOne(category, value, backend);
            }
        }
    }

    private static <T> void bindOne(ConfigCategory category, ConfigValue<T> value, ConfigBackend backend) {
        value.bind(
                () -> backend.get(category, value),
                newValue -> backend.set(category, value, newValue)
        );
    }

    public static Map<ConfigType, List<ConfigCategory>> groupByType(List<ConfigCategory> categories) {
        Map<ConfigType, List<ConfigCategory>> byType = new EnumMap<>(ConfigType.class);
        for (ConfigCategory category : categories) {
            byType.computeIfAbsent(category.getType(), key -> new ArrayList<>()).add(category);
        }
        return byType;
    }

    public static String fullKey(ConfigCategory category, ConfigValue<?> value) {
        return category.getName() + "." + value.getKey();
    }


    public static <T> T clamp(T value, ConfigValue<T> configValue) {
        if (value == null) {
            return configValue.getDefault();
        }

        T min = configValue.getMin();
        T max = configValue.getMax();
        if (min == null || max == null) {
            return value;
        }

        if (value instanceof Number num && min instanceof Number minN && max instanceof Number maxN) {
            double clamped = Math.max(minN.doubleValue(), Math.min(maxN.doubleValue(), num.doubleValue()));

            if (value instanceof Integer) return (T) Integer.valueOf((int) clamped);
            if (value instanceof Long) return (T) Long.valueOf((long) clamped);
            if (value instanceof Float) return (T) Float.valueOf((float) clamped);
            if (value instanceof Double) return (T) Double.valueOf(clamped);
        }

        return value;
    }

    public static Object clampRaw(Object rawValue, ConfigValue<?> configValue) {
        return clamp(rawValue, (ConfigValue<Object>) configValue);
    }
}