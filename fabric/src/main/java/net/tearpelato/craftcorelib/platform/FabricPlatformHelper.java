package net.tearpelato.craftcorelib.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.ResourceLocation;
import net.tearpelato.craftcorelib.platform.services.IPlatformHelper;

public class FabricPlatformHelper implements IPlatformHelper {

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

    @Override
    public String getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(modId);
    }

    @Override
    public ResourceLocation getModIcon(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(ModContainer::getMetadata)
                .flatMap(meta -> meta.getIconPath(64))
                .map(path -> {
                    String cleanPath = path;
                    String prefix = "assets/" + modId + "/";
                    if (cleanPath.startsWith(prefix)) {
                        cleanPath = cleanPath.substring(prefix.length());
                    } else if (cleanPath.startsWith("assets/")) {
                        cleanPath = cleanPath.substring("assets/".length());
                        int slash = cleanPath.indexOf('/');
                        if (slash != -1) {
                            return ResourceLocation.fromNamespaceAndPath(
                                    cleanPath.substring(0, slash),
                                    cleanPath.substring(slash + 1)
                            );
                        }
                    }
                    return ResourceLocation.fromNamespaceAndPath(modId, cleanPath);
                })
                .orElse(null);
    }
}
