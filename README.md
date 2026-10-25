# CraftCoreLIB
A Built-In library to cross-platform projects.

## Registries
ObjectRegistries is the class providing all the utils for registries. 
How to use:

```
// Custom Block
// Replace the modid and the blockname with your own
public static final ObjectRegistries<Block> MY_CUSTOM_BLOCK = ObjectRegistries.registerBlock(ResourceLocation.fromNamespaceAndPath("mod_id", "my_cool_block"),
  ()-> new Block(BlockBehaviour.Properties.of()));
```

```
//Custom Item
public static final ObjectRegistries<Item> MY_ITEM = ObjectRegistries.registerItem(ResourceLocation.fromNamespaceAndPath("mod_id", "my_cool_block"),
()-> new Item(new Item.Properties());
```
and many more registries covering menus, blockentities creativetabs and more!!

## Config System
Dedicated Config System built in to work perfectly on cross-platform:
- Translatable Configs -> Configs can be translated in all the languages you want
- Allow Strings, Ints, Boolean, Enum and more
- Built-In config screen for fabric

How To use:

```
public class Configs {

    //Configs declarations -> all the configs you have must be declared here
    public static final ConfigValue<Boolean> check;
    public static final ConfigValue<Integer> integer;

    //Config Builder -> Creating a new ConfigBuilder with you MODID with the target of configs: CLIENT,COMMON, SERVER
    public static final ConfigBuilder BUILDER = new ConfigBuilder("mod_id", ConfigType.CLIENT);
    
    //Register -> with a static method, using the builder to register all the configs, configs categories and more
    static {
        //Method Start -> creating a category
        BUILDER.start("category1");
        //Defining the comment, the name and the type of the config, int, boolean, enum, long, string, even custom configs!!
        check = BUILDER.comment("checker").defineBoolean("check", true);
        //Method End -> closing a category
        BUILDER.end();

        BUILDER.start("integer");
        // Creating an Int config with a default value with the range
        integer = BUILDER.defineInt("integer",5,0,10);
        BUILDER.end();

    }

    //Entrypoint initialization
    public static void init() {
        // Building categories with configs
        BUILDER.build();
    }
}



```
## Built-In Network builder
- Network Creation
- Network Registration
- Custom Packet handling


## And much more coming soon!!
