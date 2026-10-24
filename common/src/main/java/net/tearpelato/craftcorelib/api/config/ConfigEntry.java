package net.tearpelato.craftcorelib.api.config;

import java.util.List;
import java.util.function.Predicate;

public record ConfigEntry<T>(
        String key, String path, Kind kind, T defaultValue,
        Object min, Object max, Predicate<Object> validator,
        List<String> comment, String translation, ConfigValue<T> value) {

    public enum Kind {
        BOOLEAN,
        INT,
        LONG,
        DOUBLE,
        STRING,
        ENUM,
        GENERIC
    }
}