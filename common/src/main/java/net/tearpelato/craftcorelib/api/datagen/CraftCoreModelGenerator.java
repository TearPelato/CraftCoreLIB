package net.tearpelato.craftcorelib.api.datagen;

import com.google.common.collect.Maps;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CraftCoreModelGenerator implements DataProvider
{
    private final String name;
    private final PackOutput.PathProvider blockstatePaths;
    private final PackOutput.PathProvider itemPaths;
    private final PackOutput.PathProvider modelPaths;
    private final List<CoreGenerator.Factory<?>> factories = new ArrayList<>();

    public CraftCoreModelGenerator(PackOutput output, String modId)
    {
        this.name = "Craft Core Models (" + modId + ")";
        this.blockstatePaths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.itemPaths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        this.modelPaths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    public CraftCoreModelGenerator add(CoreGenerator.Factory<?> factory)
    {
        this.factories.add(factory);
        return this;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output)
    {
        CoreGenerator.Context context = new CoreGenerator.Context();
        this.factories.forEach(factory -> factory.create(context).generate());

        return CompletableFuture.allOf(
                DataProvider.saveAll(output, BlockStateModelDispatcher.CODEC,
                        block -> this.blockstatePaths.json(block.builtInRegistryHolder().key().identifier()),
                        Maps.transformValues(context.blocks(), BlockModelDefinitionGenerator::create)),
                DataProvider.saveAll(output, ClientItem.CODEC,
                        item -> this.itemPaths.json(item.builtInRegistryHolder().key().identifier()),
                        context.items()),
                DataProvider.saveAll(output, Supplier::get, this.modelPaths::json, context.models())
        );
    }

    @Override
    public String getName()
    {
        return this.name;
    }
}
