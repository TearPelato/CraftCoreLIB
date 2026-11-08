package net.tearpelato.craftcorelib.api.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class HolderRegistry<T> extends ObjectRegistries<T> {
    private Holder<T> holder;

    public HolderRegistry(Registry<? super T> registry, Identifier id, Supplier<T> supplier) {
        super(registry, id, supplier);
    }

    @Override
    protected void register() {
        this.holder = ((Registry<T>) this.registry).wrapAsHolder(this.instance);
    }

    public Holder<T> holder() {
        if (this.holder == null)
            throw new IllegalStateException("Entry " + id + " has not been created yet");
        return this.holder;
    }
}