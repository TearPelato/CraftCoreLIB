package net.tearpelato.craftcorelib.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.tearpelato.craftcorelib.api.network.NetworkContext;
import org.jetbrains.annotations.Nullable;

public class FabricClientContext implements NetworkContext {

    private final ClientPlayNetworking.Context ctx;

    public FabricClientContext(ClientPlayNetworking.Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void enqueue(Runnable task) {
        ctx.client().execute(task);
    }

    @Override
    public boolean isServer() {
        return false;
    }

    @Override
    public @Nullable ServerPlayer serverPlayer() {
        return null;
    }

    @Override
    public void reply(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}