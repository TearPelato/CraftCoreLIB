package net.tearpelato.craftcorelib.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tearpelato.craftcorelib.api.network.NetworkDirection;
import net.tearpelato.craftcorelib.api.network.NetworkRegistrar;
import net.tearpelato.craftcorelib.api.network.PayloadHandler;
import org.jetbrains.annotations.Nullable;

public class NeoForgeNetworkRegistrar implements NetworkRegistrar {

    private final PayloadRegistrar registrar;

    public NeoForgeNetworkRegistrar(PayloadRegistrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public <T extends CustomPacketPayload> void register(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            NetworkDirection direction,
            @Nullable PayloadHandler<T> clientHandler,
            @Nullable PayloadHandler<T> serverHandler
    ) {
        IPayloadHandler<T> neoServer = serverHandler != null
                ? (payload, ctx) -> serverHandler.handle(payload, new NeoForgeNetworkContext(ctx))
                : null;

        IPayloadHandler<T> neoClient = clientHandler != null
                ? (payload, ctx) -> clientHandler.handle(payload, new NeoForgeNetworkContext(ctx))
                : null;

        switch (direction) {
            case C2S -> registrar.playToServer(type, codec, neoServer);
            case S2C -> registrar.playToClient(type, codec, neoClient);
            case BOTH -> {
                registrar.playBidirectional(type, codec, neoServer);

            }
        }
    }
}