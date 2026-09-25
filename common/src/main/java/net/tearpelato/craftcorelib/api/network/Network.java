package net.tearpelato.craftcorelib.api.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

public final class Network {

    private static Sender SENDER = new Sender() {
        @Override
        public void sendToServer(CustomPacketPayload payload) {
            throw new IllegalStateException("Network sender not initialized (call from wrong side or before bootstrap)");
        }

        @Override
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
            throw new IllegalStateException("Network sender not initialized (call from wrong side or before bootstrap)");
        }
    };

    private Network() {}


    public static void sendToServer(CustomPacketPayload payload) {
        SENDER.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        SENDER.sendToPlayer(player, payload);
    }

    public static void sendToAllPlayers(Iterable<ServerPlayer> players, CustomPacketPayload payload) {
        for (ServerPlayer player : players) {
            sendToPlayer(player, payload);
        }
    }



    @ApiStatus.Internal
    public static void setSender(Sender sender) {
        SENDER = sender;
    }

    @ApiStatus.Internal
    public interface Sender {
        void sendToServer(CustomPacketPayload payload);
        void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
    }
}