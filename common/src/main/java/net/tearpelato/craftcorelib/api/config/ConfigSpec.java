package net.tearpelato.craftcorelib.api.config;

import java.util.List;

public record ConfigSpec(String modId, ConfigType type, List<ConfigOptions> ops) {}