package net.tearpelato.craftcorelib.test;

import net.tearpelato.craftcorelib.Constants;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigManager;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.api.config.ConfigValue;

public class ModConfig {

   public static final ConfigCategory MY_COOL_CATEGORY = ConfigCategory.create("my_cool_category", ConfigType.CLIENT)
            .title("my_cool_category");
    public static final ConfigCategory MY_COOL_CATEGORY_SERVER = ConfigCategory.create("my_category_server", ConfigType.SERVER)
            .title("my_category_server");

    public static final ConfigCategory MY_COOL_CATEGORY_COMMON = ConfigCategory.create("my_category_common", ConfigType.COMMON)
            .title("my_category_server");

    public static final ConfigValue<Boolean> MY_CONDITION = MY_COOL_CATEGORY
            .define("my_condition", false)
            //Translatable Name -> define the name inside your localisation default en_su.json
            .name("config.mymodid.mycondition")
            //Translatable Description -> define the description inside your localisation default en_su.json
            .comment("config.mymodid.mycondition.dec");

    public static final ConfigValue<Boolean> MY_CONDITION_TRUE = MY_COOL_CATEGORY
            .define("my_condition_true", true)
            //Translatable Name -> define the name inside your localisation default en_su.json
            .name("config.mymodid.myconditiontrue")
            //Translatable Description -> define the description inside your localisation default en_su.json
            .comment("config.mymodid.myconditiontrue.dec");

    public static final ConfigValue<Boolean> MY_CONDITION_WHAT = MY_COOL_CATEGORY
            .define("my_condition_what", true)
            //Translatable Name -> define the name inside your localisation default en_su.json
            .name("config.mymodid.mycondition")
            //Translatable Description -> define the description inside your localisation default en_su.json
            .comment("config.mymodid.mycondition.dec");

    public static final ConfigValue<Boolean> MY_CONDITION_WHEN = MY_COOL_CATEGORY
            .define("my_condition_when", false)
            //Translatable Name -> define the name inside your localisation default en_su.json
            .name("config.mymodid.mycondition")
            //Translatable Description -> define the description inside your localisation default en_su.json
            .comment("config.mymodid.mycondition.dec");

    public static final ConfigValue<Integer> MY_VALUE = MY_COOL_CATEGORY_SERVER
            .define("my_value", 3)
            .range(0,5)
            //Translatable Name -> define the name inside your localisation default en_su.json
            .name("config.mymodid.myvalue")
            //Translatable Description -> define the description inside your localisation default en_su.json
            .comment("config.mymodid.myvalue.dec");

    public static final ConfigValue<Integer> MY_VALUE_2 = MY_COOL_CATEGORY
            .define("my_value_2", 5)
            .range(0,15)
            //Translatable Name -> define the name inside your localisation default en_su.json
            .name("config.mymodid.myvalue")
            //Translatable Description -> define the description inside your localisation default en_su.json
            .comment("config.mymodid.myvalue.dec");

    public static final ConfigValue<Integer> MY_VALUE_COMMON = MY_COOL_CATEGORY_COMMON
            .define("my_value", 3)
            .range(0,5)
            //Translatable Name -> define the name inside your localisation default en_su.json
            .name("config.mymodid.myvalue")
            //Translatable Description -> define the description inside your localisation default en_su.json
            .comment("config.mymodid.myvalue.dec");


    public static final ConfigValue<Integer> MY_VALUE_CLIENT = MY_COOL_CATEGORY
            .define("my_value", 3)
            .range(0,5)
            //Translatable Name -> define the name inside your localisation default en_su.json
            .name("config.mymodid.myvalue")
            //Translatable Description -> define the description inside your localisation default en_su.json
            .comment("config.mymodid.myvalue.dec");


    // Accept countless categories
    public static void init() {
        ConfigManager.register(Constants.MOD_ID, MY_COOL_CATEGORY);
        ConfigManager.register(Constants.MOD_ID, MY_COOL_CATEGORY_SERVER);
        ConfigManager.register(Constants.MOD_ID, MY_COOL_CATEGORY_COMMON);
    }
}