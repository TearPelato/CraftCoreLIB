package net.tearpelato.craftcorelib.platform.services;

import net.minecraft.client.gui.screens.Screen;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface IConfigHelper {
    void register(String modId, List<ConfigCategory> categories);

    void save(String modId, ConfigType type);
    Map<String, Function<Screen, Screen>> getExternalConfigScreens();
}