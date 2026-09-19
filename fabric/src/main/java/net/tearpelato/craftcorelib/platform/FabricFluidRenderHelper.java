package net.tearpelato.craftcorelib.platform;

import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.tearpelato.craftcorelib.platform.services.IFluidRenderHelper;

public class FabricFluidRenderHelper implements IFluidRenderHelper {

    @Override
    public ResourceLocation getStillTexture(Fluid fluid) {
        return FluidVariantRendering.getSprite(FluidVariant.of(fluid)).contents().name();
    }

    @Override
    public int getTintColor(Fluid fluid, Level level, BlockPos pos) {
        return FluidVariantRendering.getColor(FluidVariant.of(fluid), level, pos);
    }

    @Override
    public RenderType getRenderLayer(FluidState state) {
        return state.getType() == Fluids.WATER ? RenderType.translucent() : RenderType.solid();
    }
}
