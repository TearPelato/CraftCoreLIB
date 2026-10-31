package net.tearpelato.craftcorelib.api.registry;

import net.minecraft.core.Registry;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.Identifier;
import net.tearpelato.craftcorelib.platform.Services;

import java.util.function.Supplier;

public class EntityDataSerializersRegistry<T> extends ObjectRegistries<EntityDataSerializer<T>> {
    public EntityDataSerializersRegistry(Identifier id, Supplier<EntityDataSerializer<T>> supplier) {
        super(null, id, supplier);
    }
    
    @Override
    protected boolean matches(Registry<?> r) {
        return r == Services.REGISTRIES.entityDataSerializerRegistry();
    }

    @Override
    protected EntityDataSerializer<T> create() {
        this.instance = this.supplier.get();
        Services.REGISTRIES.registerEntityDataSerializer(this.id, this.instance);
        return this.instance;
    }

}
