package net.tearpelato.craftcorelib.api.config;

import net.tearpelato.craftcorelib.platform.Services;

import java.util.*;

public class ConfigManager {

    private static final Map<String, List<ConfigCategory>> REGISTERED = new HashMap<>();

    public static void register(String modId, ConfigCategory... categories) {
        REGISTERED.computeIfAbsent(modId, k -> new ArrayList<>())
                .addAll(List.of(categories));

        Services.CONFIG.register(modId, List.of(categories));
    }

    public static List<ConfigCategory> getCategories(String modId) {
        return REGISTERED.getOrDefault(modId, List.of());
    }

    public static Set<String> getRegisteredModIds() {
        return REGISTERED.keySet();
    }
}