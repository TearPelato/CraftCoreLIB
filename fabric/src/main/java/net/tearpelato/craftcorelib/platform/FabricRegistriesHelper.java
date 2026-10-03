package net.tearpelato.craftcorelib.platform;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.tearpelato.craftcorelib.platform.services.IRegistriesHelper;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class FabricRegistriesHelper implements IRegistriesHelper {
    @Override
    public <T extends BlockEntity> BlockEntityType<T> create(BiFunction<BlockPos, BlockState, T> factory, Supplier<Block[]> blocks) {
        return FabricBlockEntityTypeBuilder.<T>create(factory::apply, blocks.get()).build();
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenu(BiFunction<Integer, Inventory, T> function) {
        return new MenuType<>(function::apply, FeatureFlags.DEFAULT_FLAGS);
    }
}