package net.tearpelato.craftcorelib.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.tearpelato.craftcorelib.platform.services.IRegistriesHelper;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class NeoForgeRegistriesHelper implements IRegistriesHelper {

    @Override
    public <T extends BlockEntity> BlockEntityType<T> create(BiFunction<BlockPos, BlockState, T> factory, Supplier<Block[]> blocks) {
        return BlockEntityType.Builder.<T>of(factory::apply, blocks.get()).build(null);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenu(BiFunction<Integer, Inventory, T> function) {
        return new MenuType<>(function::apply, FeatureFlags.DEFAULT_FLAGS);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuData(TriFunction<Integer, Inventory, FriendlyByteBuf, T> function) {
        return IMenuTypeExtension.create(function::apply);
    }

    @Override
    public void openMenuData(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        player.openMenu(provider, buf-> buf.writeBlockPos(pos));
    }
}
