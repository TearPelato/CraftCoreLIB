package net.tearpelato.craftcorelib.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.tearpelato.craftcorelib.api.network.Network;

public class FabricNetworkSender implements Network.Sender {
    @Override
    public void sendToServer(CustomPacketPayload payload) {
        if(FabricLoader.getInstance().getEnvironmentType() !=EnvType.CLIENT) return;
        ClientPlayNetworking.send(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
