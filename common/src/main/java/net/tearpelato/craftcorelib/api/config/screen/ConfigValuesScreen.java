package net.tearpelato.craftcorelib.api.config.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.api.config.ConfigValue;
import net.tearpelato.craftcorelib.api.config.util.ConfigBinder;
import net.tearpelato.craftcorelib.platform.Services;

import java.util.*;

public class ConfigValuesScreen extends Screen {

    private final Screen parent;
    private final String modId;
    private final ConfigType type;
    private final ConfigCategory category;

    private final Map<ConfigValue<?>, Object> originalValues = new HashMap<>();
    private final Set<ConfigValue<?>> dirty = new HashSet<>();

    private ValueList list;
    private Button saveButton;
    private Button resetButton;
    private EditBox activeEditBox;

    public ConfigValuesScreen(Screen parent, String modId, ConfigType type, ConfigCategory category) {
        super(Component.literal(Services.PLATFORM.getModName(modId) + " › " + type.name() + " › " + category.getName()));
        this.parent = parent;
        this.modId = modId;
        this.type = type;
        this.category = category;
    }

    @Override
    protected void init() {
        super.init();

        this.list = new ValueList(this.minecraft, this.width - 40, this.height - 80, 40, 26);
        this.list.setX(20);
        this.list.setEntries(this.category.getValues());
        this.addRenderableWidget(this.list);

        int btnW = 100;
        int btnH = 20;
        int y = this.height - 28;
        int center = this.width / 2;

        this.saveButton = Button.builder(Component.translatable("gui.craftcorelib.config.save"), b -> saveChanges())
                .bounds(center - btnW - 10 - btnW / 2, y, btnW, btnH)
                .build();
        this.saveButton.active = false;
        this.addRenderableWidget(this.saveButton);

        this.resetButton = Button.builder(Component.translatable("gui.craftcorelib.config.reset"), b -> resetChanges())
                .bounds(center - btnW / 2, y, btnW, btnH)
                .build();
        this.addRenderableWidget(this.resetButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(center + btnW / 2 + 10, y, btnW, btnH)
                .build());
    }

    @Override
    public void onClose() {
        if (!this.dirty.isEmpty()) {
            this.minecraft.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            saveChanges();
                        } else {
                            revertChanges();
                        }
                        this.minecraft.setScreen(this.parent);
                    },
                    Component.translatable("gui.craftcorelib.config.unsaved_title"),
                    Component.translatable("gui.craftcorelib.config.unsaved_message"),
                    Component.translatable("gui.craftcorelib.config.save"),
                    Component.translatable("gui.craftcorelib.config.discard")
            ));
            return;
        }
        this.minecraft.setScreen(this.parent);
    }

    @SuppressWarnings("unchecked")
    private static void applyValue(ConfigValue<?> value, Object newValue) {
        ((ConfigValue<Object>) value).set(newValue);
    }

    private void markDirty(ConfigValue<?> value) {
        Object original = this.originalValues.get(value);
        Object current = value.get();

        if (Objects.equals(original, current)) {
            this.dirty.remove(value);
        } else {
            this.dirty.add(value);
        }
        updateButtons();
    }

    private void updateButtons() {
        if (this.saveButton != null) {
            this.saveButton.active = !this.dirty.isEmpty();
        }
    }

    private void saveChanges() {
        Services.CONFIG.save(this.modId, this.type);
        for (ConfigValue<?> value : this.dirty) {
            this.originalValues.put(value, value.get());
        }
        this.dirty.clear();
        updateButtons();
    }

    private void revertChanges() {
        for (ConfigValue<?> value : this.dirty) {
            Object original = this.originalValues.get(value);
            if (original != null) {
                applyValue(value, original);
            }
        }
        this.dirty.clear();
        this.activeEditBox = null;
        updateButtons();
        this.list.setEntries(this.category.getValues());
    }

    private void resetChanges() {
        for (ConfigValue<?> value : this.category.getValues()) {
            try {
                this.originalValues.putIfAbsent(value, value.get());
                applyValue(value, value.getDefault());
                markDirty(value);
            } catch (Exception ignored) {}
        }
        this.activeEditBox = null;
        this.list.setEntries(this.category.getValues());
    }

    private void focusEditBox(EditBox box) {
        if (this.activeEditBox != null && this.activeEditBox != box) {
            this.activeEditBox.setFocused(false);
        }
        this.activeEditBox = box;
        if (box != null) box.setFocused(true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.activeEditBox != null && this.getFocused() == this.list
                && this.activeEditBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.activeEditBox != null && this.getFocused() == this.list
                && this.activeEditBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
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

    private static boolean isOutOfRange(Object parsed, ConfigValue<?> value) {
        Number min = value.getMin() instanceof Number n ? n : null;
        Number max = value.getMax() instanceof Number n ? n : null;
        if (min == null && max == null) return false;
        if (!(parsed instanceof Number num)) return false;

        double d = num.doubleValue();
        if (min != null && d < min.doubleValue()) return true;
        if (max != null && d > max.doubleValue()) return true;
        return false;
    }

    private static Component rangeWarningTooltip(Object parsed, ConfigValue<?> value) {
        Number min = value.getMin() instanceof Number n ? n : null;
        Number max = value.getMax() instanceof Number n ? n : null;
        if (!(parsed instanceof Number num)) {
            return Component.translatable("gui.craftcorelib.config.out_of_range");
        }
        double d = num.doubleValue();
        if (min != null && d < min.doubleValue()) {
            return Component.translatable("gui.craftcorelib.config.below_min", String.valueOf(parsed), String.valueOf(min));
        }
        if (max != null && d > max.doubleValue()) {
            return Component.translatable("gui.craftcorelib.config.above_max", String.valueOf(parsed), String.valueOf(max));
        }
        return Component.translatable("gui.craftcorelib.config.out_of_range");
    }

    private class ValueList extends ObjectSelectionList<ValueEntry> {
        public ValueList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void setEntries(List<ConfigValue<?>> values) {
            this.clearEntries();
            for (ConfigValue<?> value : values) {
                this.addEntry(new ValueEntry(value));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }
    }

    private class ValueEntry extends ObjectSelectionList.Entry<ValueEntry> {

        private final ConfigValue<?> value;
        private EditBox editBox;
        private Button toggleButton;
        private boolean unavailable;
        private boolean outOfRange;
        private Object lastParsedOutOfRange;

        @SuppressWarnings("unchecked")
        public ValueEntry(ConfigValue<?> value) {
            this.value = value;

            Object current;
            try {
                current = value.get();
            } catch (Exception e) {
                this.unavailable = true;
                return;
            }

            ConfigValuesScreen.this.originalValues.putIfAbsent(value, current);

            if (current instanceof Boolean) {
                boolean bool = (Boolean) current;
                this.toggleButton = Button.builder(
                        Component.literal(bool ? "ON" : "OFF"),
                        btn -> {
                            boolean next = !(Boolean) this.value.get();
                            ((ConfigValue<Boolean>) this.value).set(next);
                            btn.setMessage(Component.literal(next ? "ON" : "OFF"));
                            ConfigValuesScreen.this.markDirty(this.value);
                        }
                ).size(50, 18).build();
            } else if (current instanceof Number) {
                this.editBox = new EditBox(ConfigValuesScreen.this.font, 0, 0, 80, 18, Component.empty());
                this.editBox.setValue(String.valueOf(current));
                this.editBox.setFilter(s -> s.matches("-?\\d*\\.?\\d*") || s.isEmpty());
                this.editBox.setResponder(text -> {
                    if (text.isEmpty()) {
                        this.outOfRange = false;
                        this.lastParsedOutOfRange = null;
                        return;
                    }
                    try {
                        Object parsed;
                        if (current instanceof Integer) parsed = Integer.parseInt(text);
                        else if (current instanceof Long) parsed = Long.parseLong(text);
                        else if (current instanceof Float) parsed = Float.parseFloat(text);
                        else if (current instanceof Double) parsed = Double.parseDouble(text);
                        else return;

                        this.outOfRange = ConfigValuesScreen.isOutOfRange(parsed, this.value);
                        this.lastParsedOutOfRange = this.outOfRange ? parsed : null;

                        Object clamped = ConfigBinder.clampRaw(parsed, this.value);
                        ((ConfigValue<Object>) this.value).set(clamped);
                        ConfigValuesScreen.this.markDirty(this.value);
                    } catch (NumberFormatException ignored) {
                        this.outOfRange = false;
                        this.lastParsedOutOfRange = null;
                    }
                });
            }
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {

            String nameKey = this.value.getNameKey();
            Component name = nameKey != null
                    ? Component.translatable(nameKey)
                    : Component.literal(this.value.getKey());

            g.drawString(ConfigValuesScreen.this.font, name, left + 4, top + 6, 0xFFFFFFFF, true);

            if (this.unavailable) {
                g.drawString(ConfigValuesScreen.this.font,
                        Component.translatable("gui.craftcorelib.config.unavailable"),
                        left + width - 130, top + 6, 0xFF888888, false);
                return;
            }

            if (this.editBox != null) {
                int boxX = left + width - 90;
                this.editBox.setX(boxX);
                this.editBox.setY(top + 3);
                this.editBox.render(g, mouseX, mouseY, partialTick);

                if (this.outOfRange) {
                    int warnX = boxX - 16;
                    int warnY = top + 5;
                    g.drawString(ConfigValuesScreen.this.font, "⚠", warnX, warnY, 0xFFFFAA00, true);

                    if (mouseX >= warnX && mouseX <= warnX + 12
                            && mouseY >= warnY && mouseY <= warnY + 10
                            && this.lastParsedOutOfRange != null) {
                        g.renderTooltip(
                                ConfigValuesScreen.this.font,
                                ConfigValuesScreen.rangeWarningTooltip(this.lastParsedOutOfRange, this.value),
                                mouseX, mouseY
                        );
                    }
                }
            } else if (this.toggleButton != null) {
                this.toggleButton.setX(left + width - 60);
                this.toggleButton.setY(top + 3);
                this.toggleButton.render(g, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.editBox != null && this.editBox.mouseClicked(mouseX, mouseY, button)) {
                ConfigValuesScreen.this.focusEditBox(this.editBox);
                return true;
            }
            if (this.toggleButton != null && this.toggleButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            String nameKey = this.value.getNameKey();
            return nameKey != null ? Component.translatable(nameKey) : Component.literal(this.value.getKey());
        }
    }
}