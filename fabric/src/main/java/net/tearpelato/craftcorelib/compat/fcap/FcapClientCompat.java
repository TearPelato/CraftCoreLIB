package net.tearpelato.craftcorelib.compat.fcap;

import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public class FcapClientCompat {
    public static void registerScreen(String modId) {
        ConfigScreenFactoryRegistry.INSTANCE.register(modId, ConfigurationScreen::new);

    }
}
