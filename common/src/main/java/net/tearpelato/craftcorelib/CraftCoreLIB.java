package net.tearpelato.craftcorelib;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.tearpelato.craftcorelib.api.registry.ObjectRegistries;
import net.tearpelato.craftcorelib.platform.Services;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

public class CraftCoreLIB {


    /**
     * Allows the opening of {@link ObjectRegistries#registerMenuData(ResourceLocation, TriFunction)}
     * */
    public static void openMenuData(ServerPlayer player, @Nullable MenuProvider provider, BlockPos pos) {
         Services.REGISTRIES.openMenuData(player, provider, pos);
    }

}
