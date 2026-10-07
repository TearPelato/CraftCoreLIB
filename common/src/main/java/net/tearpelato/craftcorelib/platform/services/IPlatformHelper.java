package net.tearpelato.craftcorelib.platform.services;

import net.minecraft.resources.Identifier;

public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    Identifier getModIcon(String modId);
    String getModName(String modId);
}
