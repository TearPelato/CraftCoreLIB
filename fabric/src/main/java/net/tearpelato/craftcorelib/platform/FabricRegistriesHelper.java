package net.tearpelato.craftcorelib.platform;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.tearpelato.craftcorelib.platform.services.IRegistriesHelper;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuData(TriFunction<Integer, Inventory, FriendlyByteBuf, T> function) {
        return new MenuType<>((id, inv)-> {
            return function.apply(id, inv, new FriendlyByteBuf(Unpooled.buffer()));
        }, FeatureFlags.DEFAULT_FLAGS);
    }

    @Override
    public void openMenuData(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
            @Override
            public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                return provider.createMenu(i, inventory, player);
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public BlockPos getScreenOpeningData(ServerPlayer player) {
                return pos;
            }
        });
    }
}