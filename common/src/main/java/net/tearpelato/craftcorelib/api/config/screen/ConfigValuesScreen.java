package net.tearpelato.craftcorelib.api.config.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
        super(Component.literal(Services.PLATFORM.getModName(modId) + " › " + type.name() + " › " + ConfigBinder.toTitleCase(category.getName())));
        this.parent = parent;
        this.modId = modId;
        this.type = type;
        this.category = category;
    }

    @Override
    protected void init() {
        super.init();

        this.list = new ValueList(this.minecraft, this.width - 40, this.height - 80, 40, 36);
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

    private static class ConfigSlider extends AbstractSliderButton {
        private final Runnable onApply;
        private final String label;
        private final double min;
        private final double max;
        private final boolean isInteger;

        public ConfigSlider(int x, int y, int width, int height, double initialValue, String label, double min, double max, boolean isInteger, Runnable onApply) {
            super(x, y, width, height, Component.empty(), initialValue);
            this.onApply = onApply;
            this.label = label;
            this.min = min;
            this.max = max;
            this.isInteger = isInteger;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            double realValue = this.min + this.value * (this.max - this.min);

            String valueText;
            if (this.isInteger) {
                valueText = String.valueOf((int) Math.round(realValue));
            } else {
                valueText = String.format(Locale.ROOT, "%.2f", realValue);
                if (valueText.endsWith(".00")) {
                    valueText = valueText.substring(0, valueText.length() - 3);
                }
            }

            this.setMessage(Component.literal(this.label + ": " + valueText));
        }

        @Override
        protected void applyValue() {
            if (this.onApply != null) {
                this.onApply.run();
            }
        }

        public void setValueFromExternal(double normalized) {
            this.value = Mth.clamp(normalized, 0.0, 1.0);
            this.updateMessage();
        }

        public double getNormalizedValue() {
            return this.value;
        }
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
        private ConfigSlider slider;
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

                Number minN = value.getMin() instanceof Number n ? n : null;
                Number maxN = value.getMax() instanceof Number n ? n : null;
                boolean hasRange = minN != null && maxN != null;

                ConfigValue.EditorType editorType = value.getEditorType();

                boolean showBox;
                boolean showSlider;

                switch (editorType) {
                    case BOX -> {
                        showBox = true;
                        showSlider = false;
                    }
                    case SLIDER -> {
                        showBox = false;
                        showSlider = hasRange;
                    }
                    case BOTH -> {
                        showBox = true;
                        showSlider = hasRange;
                    }
                    case AUTO -> {
                        showBox = true;
                        showSlider = hasRange;
                    }
                    default -> {
                        showBox = true;
                        showSlider = false;
                    }
                }

                if (showBox) {
                    this.editBox = new EditBox(ConfigValuesScreen.this.font, 0, 0, 70, 18, Component.empty());
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

                            if (this.slider != null && clamped instanceof Number n) {
                                updateSliderFromValue(n.doubleValue());
                            }
                        } catch (NumberFormatException ignored) {
                            this.outOfRange = false;
                            this.lastParsedOutOfRange = null;
                        }
                    });
                }

                if (showSlider) {
                    final double min = minN.doubleValue();
                    final double max = maxN.doubleValue();
                    double cur = ((Number) current).doubleValue();
                    double initial = Mth.clamp((cur - min) / (max - min), 0.0, 1.0);
                    String sliderLabel;
                    String nameKey = value.getNameKey();
                    if (nameKey != null) {
                        String translated = Component.translatable(nameKey).getString();
                        sliderLabel = translated.equals(nameKey)
                                ? ConfigBinder.toTitleCase(value.getKey())
                                : translated;
                    } else {
                        sliderLabel = ConfigBinder.toTitleCase(value.getKey());
                    }

                    boolean isInteger = current instanceof Integer || current instanceof Long;

                    this.slider = new ConfigSlider(0, 0, showBox ? 100 : 140, 18, initial,
                            sliderLabel, min, max, isInteger,
                            () -> {
                                double newVal = min + this.slider.getNormalizedValue() * (max - min);

                                Object toSet;
                                if (current instanceof Integer) toSet = (int) Math.round(newVal);
                                else if (current instanceof Long) toSet = Math.round(newVal);
                                else if (current instanceof Float) toSet = (float) newVal;
                                else toSet = newVal;

                                Object clamped = ConfigBinder.clampRaw(toSet, this.value);
                                ((ConfigValue<Object>) this.value).set(clamped);
                                ConfigValuesScreen.this.markDirty(this.value);

                                if (this.editBox != null) {
                                    this.editBox.setValue(String.valueOf(clamped));
                                }
                            });
                }

            } else if (current instanceof String) {
                this.editBox = new EditBox(ConfigValuesScreen.this.font, 0, 0, 120, 18, Component.empty());
                this.editBox.setValue((String) current);
                this.editBox.setMaxLength(256);
                this.editBox.setResponder(text -> {
                    ((ConfigValue<String>) this.value).set(text);
                    ConfigValuesScreen.this.markDirty(this.value);
                });
            }
        }

        private void updateSliderFromValue(double val) {
            if (this.slider == null) return;

            Number minN = this.value.getMin() instanceof Number n ? n : null;
            Number maxN = this.value.getMax() instanceof Number n ? n : null;
            if (minN == null || maxN == null) return;

            double min = minN.doubleValue();
            double max = maxN.doubleValue();
            double normalized = Mth.clamp((val - min) / (max - min), 0.0, 1.0);

            this.slider.setValueFromExternal(normalized);
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {

            String nameKey = this.value.getNameKey();
            Component name;
            if (nameKey != null) {
                String translated = Component.translatable(nameKey).getString();
                if (!translated.equals(nameKey)) {
                    name = Component.literal(translated);
                } else {
                    name = Component.literal(ConfigBinder.toTitleCase(this.value.getKey()));
                }
            } else {
                name = Component.literal(ConfigBinder.toTitleCase(this.value.getKey()));
            }

            g.drawString(ConfigValuesScreen.this.font, name, left + 4, top + 6, 0xFFFFFFFF, true);

            if (this.unavailable) {
                g.drawString(ConfigValuesScreen.this.font,
                        Component.translatable("gui.craftcorelib.config.unavailable"),
                        left + width - 130, top + 6, 0xFF888888, false);
                return;
            }

            int right = left + width;

            if (this.slider != null) {
                this.slider.setX(right - 180);
                this.slider.setY(top + 3);
                this.slider.render(g, mouseX, mouseY, partialTick);
            }

            if (this.editBox != null) {
                int boxX = this.slider != null ? right - 70 : right - 90;
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
                this.toggleButton.setX(right - 60);
                this.toggleButton.setY(top + 3);
                this.toggleButton.render(g, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.slider != null && this.slider.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
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
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (this.slider != null && this.slider.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (this.slider != null && this.slider.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            String nameKey = this.value.getNameKey();
            if (nameKey != null) {
                return Component.translatable(nameKey);
            }
            return Component.literal(ConfigBinder.toTitleCase(this.value.getKey()));
        }
    }
}