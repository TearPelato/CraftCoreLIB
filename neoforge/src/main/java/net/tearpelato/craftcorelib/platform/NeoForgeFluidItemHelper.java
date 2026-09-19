package net.tearpelato.craftcorelib.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.tearpelato.craftcorelib.platform.services.IFluidItemHelper;

public class NeoForgeFluidItemHelper implements IFluidItemHelper {
    @Override
    public Fluid getFluidFromItemStack(ItemStack stack) {
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(stack).orElse(null);
        if (handler != null) {
            FluidStack fluidInHandler = handler.getFluidInTank(0);
            if (!fluidInHandler.isEmpty()) return fluidInHandler.getFluid();
        }
        return Fluids.EMPTY;
    }
}
