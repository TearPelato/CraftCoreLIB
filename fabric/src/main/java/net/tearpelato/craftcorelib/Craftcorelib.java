package net.tearpelato.craftcorelib;

import net.fabricmc.api.ModInitializer;
import net.tearpelato.craftcorelib.api.network.Network;
import net.tearpelato.craftcorelib.api.network.NetworkBuilder;
import net.tearpelato.craftcorelib.network.FabricNetworkRegistrar;
import net.tearpelato.craftcorelib.network.FabricNetworkSender;
import net.tearpelato.craftcorelib.test.ModConfig;

public class Craftcorelib implements ModInitializer {

    @Override
    public void onInitialize() {
        Network.setSender(new FabricNetworkSender());
        NetworkBuilder.flush(new FabricNetworkRegistrar());
        ModConfig.init();
    }
}
