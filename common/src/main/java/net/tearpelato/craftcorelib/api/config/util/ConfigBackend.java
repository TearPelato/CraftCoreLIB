package net.tearpelato.craftcorelib.api.config.util;

import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigValue;

public interface ConfigBackend {

    <T> T get(ConfigCategory category, ConfigValue<T> value);

    <T> void set(ConfigCategory category, ConfigValue<T> value, T newValue);
}
