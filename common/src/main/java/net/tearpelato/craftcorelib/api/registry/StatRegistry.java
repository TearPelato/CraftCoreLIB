package net.tearpelato.craftcorelib.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;


public class StatRegistry extends ObjectRegistries<ResourceLocation> {

    private final StatFormatter formatter;

    public StatRegistry(Registry<? super ResourceLocation> registry, ResourceLocation id, StatFormatter formatter) {
        super(registry, id, ()-> id);
        this.formatter = formatter;
    }

    @Override
    protected void register() {
        super.register();
        Stats.CUSTOM.get(this.id, this.formatter);
    }
}