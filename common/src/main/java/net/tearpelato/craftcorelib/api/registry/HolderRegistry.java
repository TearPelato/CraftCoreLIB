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
    protected T registerEntry() {
        this.holder = Registry.registerForHolder(this.registry, this.id, this.supplier.get());
        return this.holder.value();
    }

    public Holder<T> holder() {
        if (this.holder == null) {
            throw new IllegalStateException("Holder for " + this.id + " has not been registered yet");
        }

        return this.holder;
    }
}