package net.tearpelato.craftcorelib.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;
import java.util.function.Supplier;

public class BlockRegistries<T extends Block, E extends BlockItem> extends ObjectRegistries<T> {

    protected final Function<T, E> function;
    protected E intance;

    public BlockRegistries(Registry<? super T> registry, ResourceLocation id, Supplier<T> supplier, Function<T, E> function) {
        super(registry, id, supplier);
        this.function = function;
    }

    @Override
    protected T create() {
        T instance = super.create();
        this.intance = Registry.register(BuiltInRegistries.ITEM, this.id, this.function.apply(instance));
        return instance;
    }
}