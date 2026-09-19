package net.tearpelato.craftcorelib;

import net.fabricmc.api.ModInitializer;
import net.tearpelato.craftcorelib.test.ModConfigs;

public class Craftcorelib implements ModInitializer {

    @Override
    public void onInitialize() {
        ModConfigs.init();
    }
}
