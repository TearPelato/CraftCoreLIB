package net.tearpelato.craftcorelib.api.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.tearpelato.craftcorelib.platform.Services;
import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ObjectRegistries<T> implements Supplier<T> {

    private static final Map<String, List<ObjectRegistries<?>>> ENTRIES = new HashMap<>();

    protected final Identifier id;
    protected final Supplier<T> supplier;
    protected final Registry<? super T> registry;
    private T instance;

    public ObjectRegistries(Registry<? super T> registry, Identifier id, Supplier<T> supplier) {
        this.id = id;
        this.supplier = supplier;
        this.registry = registry;
        track(this);
    }

    private static void track(ObjectRegistries<?> entry) {
        ENTRIES.computeIfAbsent(entry.id.getNamespace(), k -> new ArrayList<>())
                .add(entry);
    }

    @Override
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

    /**
     * Creating a custom block with proper Item, getting the Id, and the supplier function to define the Block Properties
     * */

    public static <T extends Block> ObjectRegistries<T> registerBlock(Identifier id, Supplier<T> supplier) {
        return new BlockRegistries<>(BuiltInRegistries.BLOCK, id, supplier, t -> new BlockItem(t, new Item.Properties()));
    }

    /**
     * Creating a custom Item with the Id and the supplier for the Item Properties
     * */

    public static <T extends Item> ObjectRegistries<T> registerItem(Identifier id, Supplier<T> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.ITEM, id, supplier);
    }

    public static ObjectRegistries<CreativeModeTab> registerCreativeTab(Identifier id, Supplier<CreativeModeTab> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.CREATIVE_MODE_TAB, id, supplier);
    }

    public static <T extends BlockEntity> ObjectRegistries<BlockEntityType<T>> registerBlockEntity(Identifier id, BiFunction<BlockPos, BlockState, T> factory, Supplier<Block[]> blocks) {
        return new ObjectRegistries<>(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, ()-> Services.REGISTRIES.create(factory, blocks));
    }

    public static <T extends EntityType<?>> ObjectRegistries<T> registerEntity(Identifier id, Supplier<T> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.ENTITY_TYPE, id, supplier);
    }

    public static <T extends AbstractContainerMenu> ObjectRegistries<MenuType<T>> registerMenu(Identifier id, BiFunction<Integer, Inventory, T> function) {
        return new ObjectRegistries<>(BuiltInRegistries.MENU, id, ()-> Services.REGISTRIES.createMenu(function));
    }

    public static <T extends AbstractContainerMenu> ObjectRegistries<MenuType<T>> registerMenuData(Identifier id, TriFunction<Integer, Inventory, FriendlyByteBuf, T> function) {
        return new ObjectRegistries<>(BuiltInRegistries.MENU, id, ()-> Services.REGISTRIES.createMenuData(function));
    }

    public static <T extends Recipe<?>> ObjectRegistries<RecipeType<T>> registerRecipeType(Identifier id) {
        return new ObjectRegistries<>(BuiltInRegistries.RECIPE_TYPE, id, ()-> new RecipeType<>() {
            @Override
            public String toString() {
                return id.toString();
            }
        });
    }

    public static <T extends RecipeSerializer<?>> ObjectRegistries<T> registerRecipeSerializer(Identifier id, Supplier<T> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.RECIPE_SERIALIZER, id, supplier);
    }

    public static ObjectRegistries<SoundEvent> registerSound(Identifier id, Supplier<SoundEvent> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.SOUND_EVENT, id, supplier);
    }

    public static <T> ObjectRegistries<T> registerCustom(Registry<? super T> registry, Identifier id, Supplier<T> supplier){
        return new ObjectRegistries<>(registry, id, supplier);
    }

    public static <T extends DataComponentType<?>> ObjectRegistries<T> registerEnchantmentComponentEffect(Identifier id, Supplier<T> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, id, supplier);
    }

    public static <T extends MapCodec<EnchantmentEntityEffect>> ObjectRegistries<T> registerEnchantmentEffect(Identifier id, Supplier<T> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE, id, supplier);
    }




    //REGISTRATION HANDLER
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