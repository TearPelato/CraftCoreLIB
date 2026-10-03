package net.tearpelato.craftcorelib.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface IRegistriesHelper {

    <T extends BlockEntity> BlockEntityType<T> create(BiFunction<BlockPos, BlockState, T> factory, Supplier<Block[]> blocks);

    <T extends AbstractContainerMenu> MenuType<T> createMenu(BiFunction<Integer, Inventory, T> function);
}
