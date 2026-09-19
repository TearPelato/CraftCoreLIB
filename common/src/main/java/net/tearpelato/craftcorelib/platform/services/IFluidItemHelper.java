package net.tearpelato.craftcorelib.platform.services;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.tearpelato.craftcorelib.platform.Services;

public interface IFluidItemHelper {
    Fluid getFluidFromItemStack(ItemStack stack);

    static IFluidItemHelper get() {
        return Services.load(IFluidItemHelper.class);
    }
}