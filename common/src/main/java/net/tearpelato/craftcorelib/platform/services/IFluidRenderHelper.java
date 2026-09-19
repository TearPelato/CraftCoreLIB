package net.tearpelato.craftcorelib.platform.services;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.tearpelato.craftcorelib.platform.Services;

public interface IFluidRenderHelper {
    ResourceLocation getStillTexture(Fluid fluid);
    int getTintColor(Fluid fluid, Level level, BlockPos pos);
    RenderType getRenderLayer(FluidState state);

    static IFluidRenderHelper get() {
        return Services.load(IFluidRenderHelper.class);
    }
}
