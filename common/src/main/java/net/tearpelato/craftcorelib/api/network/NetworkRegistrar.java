package net.tearpelato.craftcorelib.api.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface NetworkRegistrar {

    <T extends CustomPacketPayload> void register(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            NetworkDirection direction,
            PayloadHandler<T> clientHandler,
            PayloadHandler<T> serverHandler
    );
}