package net.tearpelato.craftcorelib.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

public class StatRegistry extends ObjectRegistries<Identifier> {

    private final StatFormatter formatter;

    public StatRegistry(Registry<? super Identifier> registry, Identifier id, StatFormatter formatter) {
        super(registry, id, ()-> id);
        this.formatter = formatter;
    }

    @Override
    protected void register() {
        super.register();
        Stats.CUSTOM.get(this.id, this.formatter);
    }
}
