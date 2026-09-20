package net.tearpelato.craftcorelib.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigManager;
import net.tearpelato.craftcorelib.api.config.ConfigValue;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {

    private final Screen parent;
    private final String modId;

    public ConfigScreen(Screen parent, String modId) {
        super(Component.translatable(modId + ".config.title"));
        this.parent = parent;
        this.modId = modId;
    }

    @Override
    protected void init() {
        CategoryList list = new CategoryList(this.minecraft, this.width, this.height - 64, 32, 24);

        for (ConfigCategory category : ConfigManager.getCategories(modId)) {
            list.addCategory(category);
        }

        this.addRenderableWidget(list);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    private class CategoryList extends ContainerObjectSelectionList<CategoryList.Row> {

        public CategoryList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

       public void addCategory(ConfigCategory category) {
            this.addEntry(new Row(category));
        }

        @Override
        public int getRowWidth() {
            return Math.min(300, this.width - 40);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() + 10;
        }

        public class Row extends ContainerObjectSelectionList.Entry<Row> {

          private final Button button;

           public Row(ConfigCategory category) {
               Component label = category.getTitleKey() != null
                       ? Component.translatable(category.getTitleKey())
                       : Component.literal(category.getName());

                this.button = Button.builder(label, b -> {
                    Minecraft.getInstance().setScreen(
                            new CategoryScreen(ConfigScreen.this, modId, category)
                    );
                }).bounds(0, 0, 280, 20).build();
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovered, float partialTick) {
                this.button.setX(left + (width - this.button.getWidth()) / 2);
                this.button.setY(top + (height - 20) / 2);
                this.button.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(this.button);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(this.button);
            }
        }
    }

    public static class CategoryScreen extends Screen {

        private final Screen parent;
        private final String modId;
        private final ConfigCategory category;
        private ValueList list;

        public CategoryScreen(Screen parent, String modId, ConfigCategory category) {
            super(category.getTitleKey() != null
                    ? Component.translatable(category.getTitleKey())
                    : Component.literal(category.getName()));
            this.parent = parent;
            this.modId = modId;
            this.category = category;
        }

        @Override
        protected void init() {
            this.list = new ValueList(this.minecraft, this.width, this.height - 64, 32, 24);

            for (ConfigValue<?> value : category.getValues()) {
                this.list.addValue(value);
            }

            this.addRenderableWidget(this.list);

            this.addRenderableWidget(Button.builder(Component.translatable("controls.reset"), b -> this.resetAll())
                    .bounds(this.width / 2 - 155, this.height - 28, 150, 20)
                    .build());

            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                    .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
                    .build());
        }

        private void resetAll() {
            for (ConfigValue<?> value : category.getValues()) {
                resetValue(value);
            }
            this.rebuildWidgets();
        }

        @SuppressWarnings("unchecked")
        private static <T> void resetValue(ConfigValue<T> value) {
            value.set(value.getDefault());
        }

        @Override
        public void onClose() {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        }

        private static class ValueList extends ContainerObjectSelectionList<ValueList.Row> {

            ValueList(Minecraft mc, int width, int height, int y, int itemHeight) {
                super(mc, width, height, y, itemHeight);
            }

            void addValue(ConfigValue<?> value) {
                this.addEntry(Row.of(value));
            }

            @Override
            public int getRowWidth() {
                return Math.min(400, this.width - 40);
            }

            @Override
            protected int getScrollbarPosition() {
                return this.getRowLeft() + this.getRowWidth() + 10;
            }

            static class Row extends ContainerObjectSelectionList.Entry<Row> {

                private final Component label;
                private final List<AbstractWidget> children = new ArrayList<>();
                private final ConfigValue<?> value;

                private Row(Component label, ConfigValue<?> value) {
                    this.label = label;
                    this.value = value;
                }

                static <T> Row of(ConfigValue<T> value) {
                    Component name;
                    if (value.getNameKey() != null) {
                        name = Component.translatable(value.getNameKey());
                    } else {
                        String fullKey = value.getParent().getName() + "." + value.getKey();
                        name = Component.translatable(fullKey);
                    }
                    Row row = new Row(name, value);
                    row.buildWidget(value);
                    return row;
                }

                @SuppressWarnings("unchecked")
                private <T> void buildWidget(ConfigValue<T> value) {
                    Object def = value.getDefault();

                    if (def instanceof Boolean) {
                        ConfigValue<Boolean> v = (ConfigValue<Boolean>) value;
                        this.children.add(CycleButton.onOffBuilder(v.get())
                                .create(0, 0, 120, 20, Component.empty(),
                                        (btn, val) -> v.set(val)));
                        return;
                    }

                    if (def.getClass().isEnum()) {
                        ConfigValue<Enum<?>> v = (ConfigValue<Enum<?>>) value;
                        Enum<?>[] constants = ((Class<Enum<?>>) def.getClass()).getEnumConstants();
                        this.children.add(CycleButton.<Enum<?>>builder(e -> Component.literal(e.name()))
                                .withValues(constants)
                                .withInitialValue(v.get())
                                .create(0, 0, 120, 20, Component.empty(),
                                        (btn, val) -> v.set(val)));
                        return;
                    }

                    EditBox box = new EditBox(Minecraft.getInstance().font, 0, 0, 120, 20, this.label);
                    box.setValue(String.valueOf(value.get()));
                    box.setResponder(text -> applyText(value, text, box));
                    this.children.add(box);
                }

                @SuppressWarnings("unchecked")
                private static <T> void applyText(ConfigValue<T> value, String text, EditBox box) {
                    Object def = value.getDefault();
                    try {
                        Object parsed;
                        if (def instanceof Integer) parsed = Integer.parseInt(text);
                        else if (def instanceof Long) parsed = Long.parseLong(text);
                        else if (def instanceof Double) parsed = Double.parseDouble(text);
                        else if (def instanceof Float) parsed = Float.parseFloat(text);
                        else parsed = text;

                        ((ConfigValue<Object>) value).set(parsed);
                        box.setTextColor(0xFFFFFF);
                    } catch (NumberFormatException e) {
                        box.setTextColor(0xFF5555);
                    }
                }

                @Override
                public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                   int mouseX, int mouseY, boolean hovered, float partialTick) {
                    graphics.drawString(Minecraft.getInstance().font, this.label,
                            left, top + (height - 8) / 2, 0xE0E0E0);

                    if (!this.children.isEmpty()) {
                        AbstractWidget widget = this.children.get(0);
                        widget.setX(left + width - 150);
                        widget.setY(top + (height - 20) / 2);
                        widget.render(graphics, mouseX, mouseY, partialTick);
                    }
                }

                @Override
                public List<? extends GuiEventListener> children() {
                    return this.children;
                }

                @Override
                public List<? extends NarratableEntry> narratables() {
                    return this.children;
                }
            }
        }
    }
}