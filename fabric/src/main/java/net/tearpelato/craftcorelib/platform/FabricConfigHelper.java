package net.tearpelato.craftcorelib.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.tearpelato.craftcorelib.api.config.ConfigSpec;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.compat.fcap.FcapCompat;
import net.tearpelato.craftcorelib.config.FabricConfigs;
import net.tearpelato.craftcorelib.platform.services.IConfigHelper;

public class FabricConfigHelper implements IConfigHelper {

    @Override
    public void register(ConfigSpec spec) {
        boolean client = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
        if (spec.type() == ConfigType.CLIENT && !client) return;

        if (FabricLoader.getInstance().isModLoaded("forgeconfigapiport")) {
            FcapCompat.register(spec, client);
        } else {
            FabricConfigs.register(spec);
        }
    }
}