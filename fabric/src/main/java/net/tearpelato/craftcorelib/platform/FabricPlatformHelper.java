package net.tearpelato.craftcorelib.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.tearpelato.craftcorelib.platform.services.IPlatformHelper;

import java.util.HashMap;
import java.util.Map;

public class FabricPlatformHelper implements IPlatformHelper {

    private static final Map<String, Identifier> ICONS = new HashMap<>();

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

}
