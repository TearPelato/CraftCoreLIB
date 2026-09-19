package net.tearpelato.craftcorelib.api.fluid;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.tearpelato.craftcorelib.platform.services.IFluidItemHelper;

public class FluidInteractionUtils {
    public static Fluid getFluidFromItemStack(ItemStack stack) {
        if (stack.isEmpty()) return Fluids.EMPTY;
        return IFluidItemHelper.get().getFluidFromItemStack(stack);
    }
}