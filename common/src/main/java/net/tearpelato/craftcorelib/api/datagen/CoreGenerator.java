package net.tearpelato.craftcorelib.api.datagen;

import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public abstract class CoreGenerator {
    protected final Context context;

    protected CoreGenerator(Context context)
    {
        this.context = context;
    }

    public abstract void generate();

    protected final void customItem(Item item)
    {
        this.register(item, ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item)));
    }

    protected final void flatItem(Item item)
    {
        this.register(item, ItemModelUtils.plainModel(ModelTemplates.FLAT_ITEM.create(item, TextureMapping.layer0(item), this.context.models()::put)));
    }

    protected final void handheldItem(Item item)
    {
        this.register(item, ItemModelUtils.plainModel(ModelTemplates.FLAT_HANDHELD_ITEM.create(item, TextureMapping.layer0(item), this.context.models()::put)));
    }

    protected final void flatItems(Item... items)
    {
        for(Item item : items)
            this.flatItem(item);
    }

    private void register(Item item, ItemModel.Unbaked model)
    {
        this.context.items().put(item, new ClientItem(model, ClientItem.Properties.DEFAULT));
    }


    public record Context(Map<Block, BlockModelDefinitionGenerator> blocks, Map<Item, ClientItem> items, Map<Identifier, ModelInstance> models)
    {
        public Context()
        {
            this(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
    }

    @FunctionalInterface
    public interface Factory<T extends CoreGenerator>
    {
        T create(Context context);
    }
}