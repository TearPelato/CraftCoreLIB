package net.tearpelato.craftcorelib.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tearpelato.craftcorelib.api.network.NetworkContext;
import org.jetbrains.annotations.Nullable;

public class NeoForgeNetworkContext implements NetworkContext {

    private final IPayloadContext ctx;

    public NeoForgeNetworkContext(IPayloadContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void enqueue(Runnable task) {
        ctx.enqueueWork(task);
    }

    @Override
    public boolean isServer() {
        return ctx.player() instanceof ServerPlayer;
    }

    @Override
    public @Nullable ServerPlayer serverPlayer() {
        return ctx.player() instanceof ServerPlayer sp ? sp : null;
    }

    @Override
    public void reply(CustomPacketPayload payload) {
        ctx.reply(payload);
    }
}