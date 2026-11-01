package net.tearpelato.craftcorelib;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.tearpelato.craftcorelib.api.registry.ObjectRegistries;
import net.tearpelato.craftcorelib.platform.Services;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CraftCoreLIBConstants {

    public static final String MOD_ID = "craftcorelib";
    public static final String MOD_NAME = "CraftCoreLIB";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /**
     * Allows the opening of {@link ObjectRegistries#registerMenuData(ResourceLocation, TriFunction)}
     * */
    public static void openMenuData(ServerPlayer player, @Nullable MenuProvider provider, BlockPos pos) {
        Services.REGISTRIES.openMenuData(player, provider, pos);
    }
}
