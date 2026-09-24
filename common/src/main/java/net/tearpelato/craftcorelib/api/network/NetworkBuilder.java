package net.tearpelato.craftcorelib.api.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class NetworkBuilder<T extends CustomPacketPayload> {

    private static final List<PendingRegistration<?>> PENDING = new ArrayList<>();
    public static String NETWORK_VERSION = "1";

    public static void flush(NetworkRegistrar registrar) {
        for (PendingRegistration<?> pending : PENDING) {
            pending.register(registrar);
        }
        PENDING.clear();
    }

    private final CustomPacketPayload.Type<T> type;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> codec;

    private @Nullable PayloadHandler<T> clientHandler;
    private @Nullable PayloadHandler<T> serverHandler;
    private NetworkDirection direction = NetworkDirection.BOTH;

    private NetworkBuilder(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        this.type = type;
        this.codec = codec;
    }

    public static <T extends CustomPacketPayload> NetworkBuilder<T> create(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec
    ) {
        return new NetworkBuilder<>(type, codec);
    }

    public static void setNetworkVersion(String version) {
        NETWORK_VERSION = version;
    }

    public NetworkBuilder<T> handleServer(PayloadHandler<T> handler) {
        this.serverHandler = handler;
        return this;
    }

    public NetworkBuilder<T> handleClient(PayloadHandler<T> handler) {
        this.clientHandler = handler;
        return this;
    }

    public NetworkBuilder<T> direction(NetworkDirection direction) {
        this.direction = direction;
        return this;
    }

    public void register() {
        if (direction == NetworkDirection.C2S && serverHandler == null) {
            throw new IllegalStateException("C2S packet requires handleServer(): " + type.id());
        }
        if (direction == NetworkDirection.S2C && clientHandler == null) {
            throw new IllegalStateException("S2C packet requires handleClient(): " + type.id());
        }
        if (direction == NetworkDirection.BOTH && (clientHandler == null || serverHandler == null)) {
            throw new IllegalStateException("BOTH packet requires both handleServer() and handleClient(): " + type.id());
        }

        PENDING.add(new PendingRegistration<>(type, codec, direction, clientHandler, serverHandler));
    }


    private record PendingRegistration<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            NetworkDirection direction,
            @Nullable PayloadHandler<T> clientHandler,
            @Nullable PayloadHandler<T> serverHandler
    ) {
        void register(NetworkRegistrar registrar) {
            registrar.register(type, codec, direction, clientHandler, serverHandler);
        }
    }
}