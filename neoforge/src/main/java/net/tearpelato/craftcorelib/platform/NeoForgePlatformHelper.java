package net.tearpelato.craftcorelib.platform;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.tearpelato.craftcorelib.platform.services.IPlatformHelper;

import java.util.HashMap;
import java.util.Map;

public class NeoForgePlatformHelper implements IPlatformHelper {
    private static final Map<String, Identifier> ICONS = new HashMap<>();

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

}
