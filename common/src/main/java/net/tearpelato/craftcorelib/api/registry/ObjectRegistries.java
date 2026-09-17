package net.tearpelato.craftcorelib.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ObjectRegistries<T> {

    private static final Map<String, List<ObjectRegistries<?>>> ENTRIES = new HashMap<>();

    protected final ResourceLocation id;
    protected final Supplier<T> supplier;
    protected final Registry<? super T> registry;
    private T instance;

    public ObjectRegistries(Registry<? super T> registry, ResourceLocation id, Supplier<T> supplier) {
        this.id = id;
        this.supplier = supplier;
        this.registry = registry;
        track(this);
    }

    private static void track(ObjectRegistries<?> entry) {
        ENTRIES.computeIfAbsent(entry.id.getNamespace(), k -> new ArrayList<>())
                .add(entry);
    }

    public T get() {
        if (this.instance == null)
            throw new IllegalStateException("Entry " + id + " has not been created yet");
        return this.instance;
    }

    protected T create() {
        if (this.instance != null)
            throw new IllegalStateException("Entry " + id + " has already been created");
        this.instance = Registry.register(this.registry, this.id, this.supplier.get());
        return this.instance;
    }

    public static class BlockRegistries<T extends Block, E extends BlockItem> extends ObjectRegistries<T> {

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

    public static class Registrar {

        /**
         * Creating a custom block with proper Item, getting the Id, and the supplier function to define the Block Properties
         * */

        public static <T extends Block> ObjectRegistries<T> registerBlock(ResourceLocation id, Supplier<T> supplier) {
            return new BlockRegistries<>(BuiltInRegistries.BLOCK, id, supplier, t -> new BlockItem(t, new Item.Properties()));
        }

        /**
         * Creating a custom Item with the Id and the supplier for the Item Properties
         * */

        public static <T extends Item> ObjectRegistries<T> registerItem(ResourceLocation id, Supplier<T> supplier) {
            return new ObjectRegistries<>(BuiltInRegistries.ITEM, id, supplier);
        }
    }

    //Fabric Side
    public static void createAll(String modId) {
        List<ObjectRegistries<?>> entries = ENTRIES.get(modId);
        if (entries != null) {
            entries.forEach(ObjectRegistries::create);
        }
    }

    //NeoForge Side
    public static void createAll(String modId, Registry<?> registry) {
        List<ObjectRegistries<?>> entries = ENTRIES.get(modId);
        if (entries == null) return;

        entries.stream()
                .filter(e -> e.registry == registry)
                .forEach(ObjectRegistries::create);
    }
}