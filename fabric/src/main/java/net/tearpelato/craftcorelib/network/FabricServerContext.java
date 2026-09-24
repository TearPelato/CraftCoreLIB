package net.tearpelato.craftcorelib.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.tearpelato.craftcorelib.api.network.NetworkContext;
import org.jetbrains.annotations.Nullable;

public class FabricServerContext implements NetworkContext {

    private final ServerPlayNetworking.Context ctx;

    public FabricServerContext(ServerPlayNetworking.Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void enqueue(Runnable task) {
        ctx.server().execute(task);
    }

    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    public @Nullable ServerPlayer serverPlayer() {
        return ctx.player();
    }

    @Override
    public void reply(CustomPacketPayload payload) {
        ServerPlayNetworking.send(ctx.player(), payload);
    }
}

