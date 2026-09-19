package net.tearpelato.craftcorelib.platform;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.tearpelato.craftcorelib.platform.services.IFluidRenderHelper;

public class NeoForgeFluidRenderHelper implements IFluidRenderHelper {
    @Override
    public ResourceLocation getStillTexture(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getStillTexture();
    }

    @Override
    public int getTintColor(Fluid fluid, Level level, BlockPos pos) {
        FluidState state = fluid.defaultFluidState();
        return IClientFluidTypeExtensions.of(fluid).getTintColor(state, level, pos);
    }

    @Override
    public RenderType getRenderLayer(FluidState state) {
        return ItemBlockRenderTypes.getRenderLayer(state);
    }
}
