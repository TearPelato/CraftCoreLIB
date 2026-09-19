package net.tearpelato.craftcorelib.platform.services;

import net.tearpelato.craftcorelib.api.config.ConfigCategory;

import java.util.List;

public interface IConfigHelper {
    void register(String modId, List<ConfigCategory> categories);
}