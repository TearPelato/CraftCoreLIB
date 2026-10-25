package net.tearpelato.craftcorelib.api.config;

import java.util.List;

public sealed interface ConfigOptions {
    record Push(String name, List<String> comment, String translation) implements ConfigOptions {}
    record Pop() implements ConfigOptions {}
    record Entry(ConfigEntry<?> entry) implements ConfigOptions {}
}