package net.tearpelato.craftcorelib.platform;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.tearpelato.craftcorelib.platform.services.IPlatformHelper;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    @Override
    public String getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(modId);
    }

    @Override
    public Identifier getModIcon(String modId) {
        return ICONS.computeIfAbsent(modId, id -> {
            Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(id);
            if (containerOpt.isEmpty()) return null;

            ModContainer container = containerOpt.get();
            Optional<String> iconPath = container.getMetadata().getIconPath(32);
            return iconPath.flatMap(s -> container.findPath(s)
                    .map(path -> {
                        try (InputStream in = Files.newInputStream(path)) {
                            Identifier loc = Identifier.fromNamespaceAndPath("craftcorelib", "modicon/" + id);
                            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(()-> s,NativeImage.read(in)));
                            return loc;
                        } catch (Exception e) {
                            return null;
                        }
                    })).orElse(null);

        });
    }
}
