package net.tearpelato.craftcorelib.api.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface NetworkContext {

    void enqueue(Runnable task);

    boolean isServer();

    ServerPlayer serverPlayer();

    void reply(CustomPacketPayload payload);
}

