package net.tearpelato.craftcorelib.api.config.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigManager;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.api.config.util.ConfigBinder;
import net.tearpelato.craftcorelib.platform.Services;

import java.util.List;

public class ConfigTypeScreen extends Screen {

    private final Screen parent;
    private final String modId;
    private final ConfigType type;
    private CategoryList list;

    public ConfigTypeScreen(Screen parent, String modId, ConfigType type) {
        super(Component.literal(Services.PLATFORM.getModName(modId) + " › " + type.name()));
        this.parent = parent;
        this.modId = modId;
        this.type = type;
    }

    @Override
    protected void init() {
        super.init();

        List<ConfigCategory> cats = ConfigManager.getCategories(this.modId).stream()
                .filter(c -> c.getType() == this.type)
                .toList();

        this.list = new CategoryList(this.minecraft, this.width - 40, this.height - 70, 40, 28);
        this.list.setX(20);
        this.list.setCategories(cats);
        this.addRenderableWidget(this.list);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft.level == null) {
            this.renderPanorama(graphics, partialTick);
        } else {
            this.renderTransparentBackground(graphics);
        }
    }

    private class CategoryList extends ObjectSelectionList<CategoryEntry> {

        public CategoryList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void setCategories(List<ConfigCategory> categories) {
            this.clearEntries();
            for (ConfigCategory cat : categories) {
                this.addEntry(new CategoryEntry(cat));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }
    }

    private class CategoryEntry extends ObjectSelectionList.Entry<CategoryEntry> {

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
        public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {

            int bgColor = hovering ? 0x80FFFFFF : 0x40000000;
            g.fill(left - 2, top - 1, left + width + 2, top + height + 1, bgColor);

            g.drawString(ConfigTypeScreen.this.font, "📁  " + this.displayName,
                    left + 8, top + 8, 0xFFFFFFFF, true);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            ConfigTypeScreen.this.minecraft.setScreen(
                    new ConfigValuesScreen(ConfigTypeScreen.this, ConfigTypeScreen.this.modId,
                            ConfigTypeScreen.this.type, this.category));
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.displayName);
        }
    }
}