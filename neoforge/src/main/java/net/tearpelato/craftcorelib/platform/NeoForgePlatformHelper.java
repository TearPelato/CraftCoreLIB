package net.tearpelato.craftcorelib.platform;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.tearpelato.craftcorelib.platform.services.IPlatformHelper;

import java.util.Optional;

public class NeoForgePlatformHelper implements IPlatformHelper {

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
        return !FMLLoader.isProduction();
    }

    @Override
    public String getModName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    @Override
    public ResourceLocation getModIcon(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> {
                    Optional<String> logo = container.getModInfo().getLogoFile();
                    if (logo.isPresent()) {
                        String path = logo.get();
                        return path.contains(":")
                                ? ResourceLocation.tryParse(path)
                                : ResourceLocation.fromNamespaceAndPath(modId, path);
                    }
                    return null;
                })
                .orElse(null);
    }
}
