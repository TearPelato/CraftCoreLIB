package net.tearpelato.craftcorelib.test;

import net.tearpelato.craftcorelib.CraftCoreLIBConstants;
import net.tearpelato.craftcorelib.api.config.ConfigBuilder;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.api.config.ConfigValue;

public class Configs {

    public static final ConfigValue<Boolean> check;
    public static final ConfigValue<Integer> integer;

    public static final ConfigBuilder BUILDER = new ConfigBuilder(CraftCoreLIBConstants.MOD_ID, ConfigType.CLIENT);

    static {
        BUILDER.start("category1");
        check = BUILDER.comment("checker").defineBoolean("check", true);
        BUILDER.end();

        BUILDER.start("integer");
        integer = BUILDER.defineInt("integer",5,0,10);
        BUILDER.end();

    }

    public static void init() {
        BUILDER.build();
    }
}
