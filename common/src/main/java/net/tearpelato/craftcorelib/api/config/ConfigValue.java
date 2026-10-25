package net.tearpelato.craftcorelib.api.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigValue<T> {
    private final String path;
    private final T defaultValue;
    private volatile Supplier<T> getter;
    private volatile Consumer<T> setter;
    private volatile Runnable saver;

    ConfigValue(String path, T defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public T get() {
        Supplier<T> g = getter;
        if (g == null) throw new IllegalStateException("Config '" + path + "' is not registered yet");
        return g.get();
    }

    public void set(T value) {
        Consumer<T> s = setter;
        if (s == null) throw new IllegalStateException("Config '" + path + "' is not registered yet");
        s.accept(value);
    }

    public void save() {
        Runnable s = saver;
        if (s != null) s.run();
    }

    public T getDefault() { return defaultValue; }
    public String getPath() { return path; }

    public void bind(Supplier<T> getter, Consumer<T> setter, Runnable saver) {
        this.getter = getter;
        this.setter = setter;
        this.saver = saver;
    }
}