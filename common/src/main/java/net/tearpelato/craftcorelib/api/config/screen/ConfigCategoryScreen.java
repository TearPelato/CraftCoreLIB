package net.tearpelato.craftcorelib.api.config.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.api.config.util.ConfigBinder;
import net.tearpelato.craftcorelib.platform.Services;

import java.util.List;

public class ConfigCategoryScreen extends Screen {
    private final Screen parent;
    private final String modId;
    private final ConfigType type;
    private final ConfigCategory category;
    private CategoryList list;

    public ConfigCategoryScreen(Screen parent, String modId, ConfigType type, ConfigCategory category) {
        super(Component.literal(
                Services.PLATFORM.getModName(modId) + " › " + type.name()
                        + " › " + ConfigBinder.toTitleCase(category.getName())));
        this.parent = parent;
        this.modId = modId;
        this.type = type;
        this.category = category;
    }

    @Override
    protected void init() {
        super.init();

        List<ConfigCategory> children = this.category.getChildren();

        this.list = new CategoryList(this.minecraft, this.width - 40, this.height - 70, 40, 28);
        this.list.setCategories(children);
        this.addRenderableWidget(this.list);



        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, a);
        } else {
            this.extractTransparentBackground(graphics);
        }
    }

    private class CategoryList extends ObjectSelectionList<ConfigCategoryScreen.CategoryEntry> {

        public CategoryList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void setCategories(List<ConfigCategory> categories) {
            this.clearEntries();
            for (ConfigCategory cat : categories) {
                this.addEntry(new ConfigCategoryScreen.CategoryEntry(cat));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }
    }

    private class CategoryEntry extends ObjectSelectionList.Entry<ConfigCategoryScreen.CategoryEntry> {

        private final ConfigCategory category;
        private final String displayName;

        CategoryEntry(ConfigCategory category) {
            this.category = category;

            String display = category.getName();
            if (category.getTitleKey() != null) {
                String translated = Component.translatable(category.getTitleKey()).getString();
                if (!translated.equals(category.getTitleKey())) {
                    display = translated;
                } else {
                    display = ConfigBinder.toTitleCase(category.getName());
                }
            } else {
                display = ConfigBinder.toTitleCase(category.getName());
            }
            this.displayName = display;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = getContentX();
            int y = getContentY();
            int w = getContentWidth();
            int h = getContentHeight();

            int bgColor = hovered ? 0x80FFFFFF : 0x40000000;
            g.fill(x - 2, y - 1, x + w + 2, y + h + 1, bgColor);

            g.text(ConfigCategoryScreen.this.font, this.displayName,
                    x + 8, y + (h - 8) / 2, 0xFFFFFFFF, true);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            ConfigCategoryScreen.this.minecraft.gui.setScreen(
                    new ConfigValuesScreen(ConfigCategoryScreen.this, ConfigCategoryScreen.this.modId,
                            ConfigCategoryScreen.this.type, this.category));
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.displayName);
        }
    }
}
