package net.tearpelato.craftcorelib.platform;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.tearpelato.craftcorelib.platform.services.IPlatformHelper;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class NeoForgePlatformHelper implements IPlatformHelper {
    private static final Map<String, ResourceLocation> ICONS = new HashMap<>();

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
        return ICONS.computeIfAbsent(modId, id -> ModList.get().getModContainerById(id)
                .map(ModContainer::getModInfo)
                .flatMap(info -> info.getLogoFile().map(logo -> {
                    try (InputStream in = Files.newInputStream(info.getOwningFile().getFile().findResource(logo))) {
                        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("craftcorelib", "modicon/" + id);
                        Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(NativeImage.read(in)));
                        return loc;
                    } catch (Exception e) {
                        return null;
                    }
                })).orElse(null));
    }
}
