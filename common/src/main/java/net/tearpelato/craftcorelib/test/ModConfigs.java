package net.tearpelato.craftcorelib.test;

import net.tearpelato.craftcorelib.Constants;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigManager;
import net.tearpelato.craftcorelib.api.config.ConfigValue;

public class ModConfigs {
    public static final ConfigCategory SINK = ConfigCategory.create("fluids")
            .title("conifg.mymod.fluidcategory")
            .comment("config.mymod.fluidcategory_dec");


    public static final ConfigValue<Integer> CAPACITY = SINK
            .define("capacity", 100)
            .comment("");

    public static void init() {
        ConfigManager.register(Constants.MOD_ID, SINK);
    }
}
