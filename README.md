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

## Render Utils: Fluid
 - Fluid Container creation
 - Fluid Render
 - Fluid Interactions
