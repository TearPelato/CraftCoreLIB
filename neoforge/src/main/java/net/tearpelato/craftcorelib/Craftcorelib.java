package net.tearpelato.craftcorelib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.tearpelato.craftcorelib.test.Configs;

@Mod(CraftCoreLIBConstants.MOD_ID)
public class Craftcorelib {

    public Craftcorelib(IEventBus modBus) {
        Configs.init();
    }
}
