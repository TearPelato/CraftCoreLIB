package net.tearpelato.craftcorelib.test;

import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigManager;
import net.tearpelato.craftcorelib.api.config.ConfigValue;

public class ModConfig {

    public static final ConfigCategory MY_COOL_CATEGORY = ConfigCategory.create("my_cool_category")
            .title("my_cool_category");


    public static final ConfigValue<Boolean> MY_CONDITION = MY_COOL_CATEGORY
            .define("my_condition", false)
            //Translatable Name -> define the name inside your localisation default en_su.json
            .name("config.mymodid.mycondition")
            //Translatable Description -> define the description inside your localisation default en_su.json
            .comment("config.mymodid.mycondition.dec");

    // Accept countless categories
    public static void init() {
        ConfigManager.register("mymodid", MY_COOL_CATEGORY);
    }
}
