package net.tearpelato.craftcorelib.api.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigValue<T> {

    private final String key;
    private final T defaultValue;
    private final ConfigCategory parent;
    private String commentKey;
    private T min, max;
    private Supplier<T> getter;
    private Consumer<T> setter;

    ConfigValue(ConfigCategory parent, String key, T defaultValue) {
        this.parent = parent;
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public ConfigValue<T> comment(String translationKey) {
        this.commentKey = translationKey;
        return this;
    }

    public ConfigValue<T> range(T min, T max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public T get() {
        return getter != null ? getter.get() : defaultValue;
    }

    public void set(T value) {
        if (setter != null) setter.accept(value);
    }

    public void bind(Supplier<T> getter, Consumer<T> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public String getKey() {
        return key;
    }

    public T getDefault() {
        return defaultValue;
    }

    public String getCommentKey() {
        return commentKey;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }
}
