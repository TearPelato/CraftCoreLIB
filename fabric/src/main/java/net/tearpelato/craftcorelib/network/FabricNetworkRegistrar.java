package net.tearpelato.craftcorelib.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tearpelato.craftcorelib.api.network.NetworkDirection;
import net.tearpelato.craftcorelib.api.network.NetworkRegistrar;
import net.tearpelato.craftcorelib.api.network.PayloadHandler;
import org.jetbrains.annotations.Nullable;

public class FabricNetworkRegistrar implements NetworkRegistrar {

    @Override
    public <T extends CustomPacketPayload> void register(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            NetworkDirection direction,
            @Nullable PayloadHandler<T> clientHandler,
            @Nullable PayloadHandler<T> serverHandler
    ) {
        switch (direction) {
            case C2S -> {
                PayloadTypeRegistry.playC2S().register(type, codec);
                ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                        serverHandler.handle(payload, new FabricServerContext(context)));
            }
            case S2C -> {
                PayloadTypeRegistry.playS2C().register(type, codec);
                registerClientReceiver(type, clientHandler);
            }
            case BOTH -> {
                PayloadTypeRegistry.playC2S().register(type, codec);
                PayloadTypeRegistry.playS2C().register(type, codec);

                ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                        serverHandler.handle(payload, new FabricServerContext(context)));

                registerClientReceiver(type, clientHandler);
            }
        }
    }

    private static <T extends CustomPacketPayload> void registerClientReceiver(
            CustomPacketPayload.Type<T> type,
            PayloadHandler<T> handler
    ) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                    handler.handle(payload, new FabricClientContext(context)));
        }
    }
}