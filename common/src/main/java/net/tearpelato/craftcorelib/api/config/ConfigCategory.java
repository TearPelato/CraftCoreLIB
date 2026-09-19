package net.tearpelato.craftcorelib.api.config;

import java.util.ArrayList;
import java.util.List;

public class ConfigCategory {

    private final String name;
    private String titleKey;
    private String commentKey;
    private final List<ConfigValue<?>> values = new ArrayList<>();

    private ConfigCategory(String name) {
        this.name = name;
    }

    public static ConfigCategory create(String name) {
        return new ConfigCategory(name);
    }

    public ConfigCategory title(String translationKey) {
        this.titleKey = translationKey;
        return this;
    }

    public ConfigCategory comment(String translationKey) {
        this.commentKey = translationKey;
        return this;
    }

    public <T> ConfigValue<T> define(String key, T defaultValue) {
        ConfigValue<T> value = new ConfigValue<>(this, key, defaultValue);
        values.add(value);
        return value;
    }

    public String getName() {
        return name;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getCommentKey() {
        return commentKey;
    }

    public List<ConfigValue<?>> getValues() {
        return values;
    }
}