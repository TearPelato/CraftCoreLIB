package net.tearpelato.craftcorelib.api.config;

public interface ConfigBackend {

    <T> T get(ConfigCategory category, ConfigValue<T> value);

    <T> void set(ConfigCategory category, ConfigValue<T> value, T newValue);
}
