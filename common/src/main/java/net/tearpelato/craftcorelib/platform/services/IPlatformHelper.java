package net.tearpelato.craftcorelib.platform.services;

import net.minecraft.resources.ResourceLocation;
import net.tearpelato.craftcorelib.api.config.ConfigType;

public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    ResourceLocation getModIcon(String modId);
    String getModName(String modId);
}
