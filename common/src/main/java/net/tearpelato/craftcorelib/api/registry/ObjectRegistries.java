package net.tearpelato.craftcorelib.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public static ObjectRegistries<CreativeModeTab> registerCreativeTab(ResourceLocation id, Supplier<CreativeModeTab> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.CREATIVE_MODE_TAB, id, supplier);
    }

    public static ObjectRegistries<BlockEntityType<?>> registerBlockEntity(ResourceLocation id, Supplier<BlockEntityType<?>> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, supplier);
    }

    public static ObjectRegistries<EntityType<?>> registerEntity(ResourceLocation id, Supplier<EntityType<?>> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.ENTITY_TYPE, id, supplier);
    }

    public static ObjectRegistries<MenuType<?>> registerMenu(ResourceLocation id, Supplier<MenuType<?>> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.MENU, id, supplier);
    }

    public static ObjectRegistries<RecipeType<?>> registerRecipeType(ResourceLocation id) {
        return new ObjectRegistries<>(BuiltInRegistries.RECIPE_TYPE, id, ()-> new RecipeType<>() {
            @Override
            public String toString() {
                return id.toString();
            }
        });
    }

    public static ObjectRegistries<RecipeSerializer<?>> registerRecipeSerializer(ResourceLocation id, Supplier<RecipeSerializer<?>> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.RECIPE_SERIALIZER, id, supplier);
    }

    public static ObjectRegistries<SoundEvent> registerSound(ResourceLocation id, Supplier<SoundEvent> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.SOUND_EVENT, id, supplier);
    }

    public static <T> ObjectRegistries<T> registerCustom(Registry<? super T> registry, ResourceLocation id, Supplier<T> supplier){
        return new ObjectRegistries<>(registry, id, supplier);
    }

    public static <T extends DataComponentType<?>> ObjectRegistries<T> registerEnchantmentEffect(ResourceLocation id, Supplier<T> supplier) {
        return new ObjectRegistries<>(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, id, supplier);
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