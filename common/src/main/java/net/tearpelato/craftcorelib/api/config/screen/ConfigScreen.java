package net.tearpelato.craftcorelib.api.config.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigManager;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.api.config.ConfigValue;
import net.tearpelato.craftcorelib.api.config.util.ConfigBinder;
import net.tearpelato.craftcorelib.platform.Services;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

public class ConfigScreen extends Screen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private record Target(String modId, ConfigType type) {}
    private record Icon(ResourceLocation location, int width, int height) {}

    private static final Icon FALLBACK_ICON = new Icon(ResourceLocation.withDefaultNamespace("textures/misc/unknown_pack.png"), 64, 64);

    private final Screen parent;

    private final Map<ConfigValue<?>, Target> dirty = new HashMap<>();
    private final Map<ConfigValue<?>, Object> originalValues = new HashMap<>();
    private List<ConfigValue<?>> visibleValues = List.of();

    private Map<String, Function<Screen, Screen>> externalScreens = Map.of();
    private final Map<String, Icon> iconCache = new HashMap<>();

    private static final int LEFT_PANEL_WIDTH = 140;
    private static final int TOP_BAR_HEIGHT = 32;

    private static final int ACTION_BTN_WIDTH = 80;
    private static final int ACTION_BTN_HEIGHT = 20;
    private static final int ACTION_BTN_MARGIN = 10;

    private EditBox searchBox;
    private ModListWidget modList;

    private String selectedModId = null;
    private ConfigType selectedType = null;

    private final List<Button> typeButtons = new ArrayList<>();
    private Button resetButton;
    private Button saveButton;
    private ConfigEntryList entryList;
    private EditBox activeConfigEditBox;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("gui.craftcorelib.config.title"));
        this.parent = parent;
    }

    public ConfigScreen(Component title) {
        this(Minecraft.getInstance().screen);
    }

    @Override
    protected void init() {
        super.init();

        int actionY = this.height - ACTION_BTN_MARGIN - ACTION_BTN_HEIGHT;
        int saveX = this.width - ACTION_BTN_MARGIN - ACTION_BTN_WIDTH;
        int resetX = saveX - ACTION_BTN_MARGIN - ACTION_BTN_WIDTH;

        this.searchBox = new EditBox(this.font,
                LEFT_PANEL_WIDTH + 10, 8,
                this.width - LEFT_PANEL_WIDTH - 20, 16,
                Component.translatable("gui.craftcorelib.config.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setHint(Component.translatable("gui.craftcorelib.config.search_hint"));
        this.searchBox.setResponder(this::filterMods);
        this.addRenderableWidget(this.searchBox);

        this.modList = new ModListWidget(this.minecraft,
                LEFT_PANEL_WIDTH,
                this.height - TOP_BAR_HEIGHT - 10,
                TOP_BAR_HEIGHT,
                28);
        this.modList.setX(0);
        this.addRenderableWidget(this.modList);

        this.resetButton = Button.builder(Component.translatable("gui.craftcorelib.config.reset"), btn -> resetChanges())
                .bounds(resetX, actionY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT)
                .build();
        this.addRenderableWidget(this.resetButton);

        this.saveButton = Button.builder(Component.translatable("gui.craftcorelib.config.save"), btn -> saveChanges())
                .bounds(saveX, actionY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT)
                .build();
        this.saveButton.active = false;
        this.addRenderableWidget(this.saveButton);

        this.modList.refresh(collectMods());

        updateRightPanel();
    }

    private Set<String> collectMods() {
        Set<String> own = ConfigManager.getRegisteredModIds();

        Map<String, Function<Screen, Screen>> external = new LinkedHashMap<>();
        try {
            external.putAll(Services.CONFIG.getExternalConfigScreens());
        } catch (Throwable t) {
            LOGGER.warn("Unable to collect external config screens", t);
        }
        external.keySet().removeAll(own);
        this.externalScreens = external;

        Set<String> all = new TreeSet<>(
                Comparator.comparing((String id) -> Services.PLATFORM.getModName(id).toLowerCase(Locale.ROOT))
                        .thenComparing(Comparator.naturalOrder()));
        all.addAll(own);
        all.addAll(external.keySet());
        return all;
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
            this.dirty.put(value, new Target(this.selectedModId, this.selectedType));
        }
        updateActionButtons();
    }

    private void updateActionButtons() {
        if (this.saveButton != null) {
            this.saveButton.active = !this.dirty.isEmpty();
        }
        if (this.resetButton != null) {
            this.resetButton.active = this.entryList != null && !this.visibleValues.isEmpty();
        }
    }

    private void saveChanges() {
        Set<Target> targets = new HashSet<>(this.dirty.values());
        for (Target t : targets) {
            Services.CONFIG.save(t.modId(), t.type());
        }

        for (ConfigValue<?> value : this.dirty.keySet()) {
            this.originalValues.put(value, value.get());
        }

        this.dirty.clear();
        updateActionButtons();
    }

    private void revertChanges() {
        for (ConfigValue<?> value : this.dirty.keySet()) {
            Object original = this.originalValues.get(value);
            if (original != null) {
                applyValue(value, original);
            }
        }
        this.dirty.clear();
        this.activeConfigEditBox = null;
        updateActionButtons();
        updateRightPanel();
    }

    private void resetChanges() {
        for (ConfigValue<?> value : this.visibleValues) {
            try {
                this.originalValues.putIfAbsent(value, value.get());
                applyValue(value, value.getDefault());
                markDirty(value);
            } catch (IllegalStateException ignored) {
            }
        }
        this.activeConfigEditBox = null;
        updateRightPanel();
    }

    private void filterMods(String query) {
        this.modList.filter(query);
    }

    public void selectMod(String modId) {
        Function<Screen, Screen> external = this.externalScreens.get(modId);
        if (external != null) {
            openExternal(modId, external);
            return;
        }

        if (modId.equals(this.selectedModId)) {
            this.selectedModId = null;
            this.selectedType = null;
        } else {
            this.selectedModId = modId;
            this.selectedType = null;
        }
        updateRightPanel();
    }

    private void openExternal(String modId, Function<Screen, Screen> factory) {
        try {
            Screen screen = factory.apply(this);
            if (screen != null) {
                this.minecraft.setScreen(screen);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to open config screen for mod {}", modId, e);
        }
    }

    public void selectType(ConfigType type) {
        if (type == this.selectedType) {
            this.selectedType = null;
        } else {
            this.selectedType = type;
        }
        updateRightPanel();
    }

    private void updateRightPanel() {
        this.typeButtons.forEach(this::removeWidget);
        this.typeButtons.clear();

        if (this.entryList != null) {
            this.removeWidget(this.entryList);
            this.entryList = null;
        }
        this.visibleValues = List.of();
        this.activeConfigEditBox = null;

        if (this.selectedModId == null) {
            updateActionButtons();
            return;
        }

        List<ConfigCategory> categories = ConfigManager.getCategories(this.selectedModId);
        if (categories.isEmpty()) {
            updateActionButtons();
            return;
        }

        boolean hasClient = false, hasServer = false, hasCommon = false;
        for (ConfigCategory cat : categories) {
            switch (cat.getType()) {
                case CLIENT -> hasClient = true;
                case SERVER -> hasServer = true;
                case COMMON -> hasCommon = true;
            }
        }

        int rightX = LEFT_PANEL_WIDTH + 20;
        int y = TOP_BAR_HEIGHT + 60;
        int btnWidth = 90;
        int btnHeight = 24;
        int gap = 8;
        int startX = rightX;

        if (hasClient) {
            Button btn = createTypeButton(ConfigType.CLIENT, startX, y, btnWidth, btnHeight, true, null);
            this.typeButtons.add(btn);
            this.addRenderableWidget(btn);
            startX += btnWidth + gap;
        }
        if (hasServer) {
            boolean serverAvailable = this.minecraft.level != null;
            Component tooltip = serverAvailable ? null
                    : Component.translatable("gui.craftcorelib.config.server_unavailable");
            Button btn = createTypeButton(ConfigType.SERVER, startX, y, btnWidth, btnHeight, serverAvailable, tooltip);
            this.typeButtons.add(btn);
            this.addRenderableWidget(btn);
            startX += btnWidth + gap;
        }
        if (hasCommon) {
            Button btn = createTypeButton(ConfigType.COMMON, startX, y, btnWidth, btnHeight, true, null);
            this.typeButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        if (this.selectedType != null) {
            List<ConfigValue<?>> values = new ArrayList<>();
            for (ConfigCategory cat : categories) {
                if (cat.getType() == this.selectedType) {
                    values.addAll(cat.getValues());
                }
            }
            this.visibleValues = values;

            int listY = y + btnHeight + 12;
            int listBottom = this.height - ACTION_BTN_MARGIN - ACTION_BTN_HEIGHT - 8;
            int listHeight = Math.max(ACTION_BTN_HEIGHT, listBottom - listY);

            this.entryList = new ConfigEntryList(this.minecraft,
                    this.width - LEFT_PANEL_WIDTH - 40,
                    listHeight,
                    listY,
                    26);
            this.entryList.setX(rightX);
            this.entryList.setEntries(values);
            this.addRenderableWidget(this.entryList);
        }

        bringActionButtonsToFront();
        updateActionButtons();
    }

    private void bringActionButtonsToFront() {
        if (this.resetButton != null) {
            this.removeWidget(this.resetButton);
            this.addRenderableWidget(this.resetButton);
        }
        if (this.saveButton != null) {
            this.removeWidget(this.saveButton);
            this.addRenderableWidget(this.saveButton);
        }
    }

    private void focusConfigEditBox(EditBox box) {
        if (this.activeConfigEditBox != null && this.activeConfigEditBox != box) {
            this.activeConfigEditBox.setFocused(false);
        }
        this.activeConfigEditBox = box;
        if (box != null) box.setFocused(true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.activeConfigEditBox != null && this.getFocused() == this.entryList
                && this.activeConfigEditBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.activeConfigEditBox != null && this.getFocused() == this.entryList
                && this.activeConfigEditBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private Button createTypeButton(ConfigType type, int x, int y, int w, int h,
                                    boolean enabled, Component disabledTooltip) {
        Component label = Component.literal(type.name());
        Button btn = Button.builder(label, b -> selectType(type))
                .bounds(x, y, w, h)
                .build();
        btn.active = enabled;
        if (!enabled && disabledTooltip != null) {
            btn.setTooltip(Tooltip.create(disabledTooltip));
        }
        return btn;
    }


    private Icon resolveIcon(String modId) {
        return this.iconCache.computeIfAbsent(modId, id -> {
            ResourceLocation location = null;
            try {
                location = Services.PLATFORM.getModIcon(id);
            } catch (Exception ignored) {}

            if (location != null) {
                Icon icon = probeIcon(location);
                if (icon != null) return icon;
            }
            return FALLBACK_ICON;
        });
    }


    private Icon probeIcon(ResourceLocation location) {
        AbstractTexture registered = this.minecraft.getTextureManager().getTexture(location, null);
        if (registered instanceof DynamicTexture dyn && dyn.getPixels() != null) {
            return new Icon(location, dyn.getPixels().getWidth(), dyn.getPixels().getHeight());
        }

        Optional<Resource> resource = this.minecraft.getResourceManager().getResource(location);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                return new Icon(location, image.getWidth(), image.getHeight());
            } catch (IOException ignored) {}
        }
        return null;
    }

    private void drawIcon(GuiGraphics g, String modId, int x, int y, int size) {
        Icon icon = resolveIcon(modId);
        g.blit(icon.location(), x, y, size, size, 0f, 0f,
                icon.width(), icon.height(), icon.width(), icon.height());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawString(this.font, this.title, 10, 10, 0xFFFFFF, true);

        if (this.selectedModId != null) {
            int logoX = LEFT_PANEL_WIDTH + 16;
            int logoY = TOP_BAR_HEIGHT + 10;
            int logoSize = 32;

            drawIcon(graphics, this.selectedModId, logoX, logoY, logoSize);

            String displayName = Services.PLATFORM.getModName(this.selectedModId);
            graphics.drawString(this.font, displayName,
                    logoX + logoSize + 8, logoY + 10,
                    0xFFFFFFFF, true);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft.level == null) {
            this.renderPanorama(graphics, partialTick);
        } else {
            this.renderTransparentBackground(graphics);
        }
    }

    public class ModListWidget extends ObjectSelectionList<ModListWidget.Entry> {

        private final List<String> allMods = new ArrayList<>();

        public ModListWidget(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void refresh(Collection<String> modIds) {
            this.allMods.clear();
            this.allMods.addAll(modIds);
            this.clearEntries();
            for (String modId : this.allMods) {
                this.addEntry(new Entry(modId));
            }
        }

        public void filter(String query) {
            this.clearEntries();
            String q = query.toLowerCase(Locale.ROOT).trim();
            for (String modId : this.allMods) {
                String name = Services.PLATFORM.getModName(modId).toLowerCase(Locale.ROOT);
                if (q.isEmpty() || name.contains(q) || modId.toLowerCase(Locale.ROOT).contains(q)) {
                    this.addEntry(new Entry(modId));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 12;
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String modId;
            private final String displayName;

            public Entry(String modId) {
                this.modId = modId;
                this.displayName = Services.PLATFORM.getModName(modId);
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {

                boolean selected = this.modId.equals(ConfigScreen.this.selectedModId);

                int x1 = left - 2 + 1;
                int x2 = left - 2 + width - 1;
                int y1 = top - 2 + 1;
                int y2 = top + height + 2 - 1;

                if (selected) {
                    g.fill(x1, y1, x2, y2, 0x80FFFFFF);
                } else if (hovering) {
                    g.fill(x1, y1, x2, y2, 0x40FFFFFF);
                }

                ConfigScreen.this.drawIcon(g, this.modId, left + 4, top + 4, 16);

                g.drawString(ConfigScreen.this.font, this.displayName,
                        left + 24, top + 8, 0xFFFFFFFF, true);

                if (ConfigScreen.this.externalScreens.containsKey(this.modId)) {
                    g.drawString(ConfigScreen.this.font, ">",
                            left + width - 12, top + 8, 0xFFAAAAAA, false);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                ConfigScreen.this.selectMod(this.modId);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.displayName);
            }
        }
    }

    public class ConfigEntryList extends ObjectSelectionList<ConfigEntryList.Entry> {

        public ConfigEntryList(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void setEntries(List<ConfigValue<?>> values) {
            this.clearEntries();
            for (ConfigValue<?> value : values) {
                this.addEntry(new Entry(value));
            }
        }

        @Override
        public int getRowWidth() {
            return this.width - 12;
        }

        public class Entry extends ObjectSelectionList.Entry<Entry> {

            private final ConfigValue<?> value;
            private EditBox editBox;
            private Button toggleButton;
            private boolean unavailable;

            @SuppressWarnings("unchecked")
            public Entry(ConfigValue<?> value) {
                this.value = value;

                Object current;
                try {
                    current = value.get();
                } catch (IllegalStateException e) {
                    this.unavailable = true;
                    return;
                }

                ConfigScreen.this.originalValues.putIfAbsent(value, current);

                if (current instanceof Boolean) {
                    boolean bool = (Boolean) current;
                    this.toggleButton = Button.builder(
                            Component.literal(bool ? "ON" : "OFF"),
                            btn -> {
                                boolean next = !(Boolean) this.value.get();
                                ((ConfigValue<Boolean>) this.value).set(next);
                                btn.setMessage(Component.literal(next ? "ON" : "OFF"));
                                ConfigScreen.this.markDirty(this.value);
                            }
                    ).size(50, 18).build();
                } else if (current instanceof Number) {
                    this.editBox = new EditBox(ConfigScreen.this.font, 0, 0, 80, 18, Component.empty());
                    this.editBox.setValue(String.valueOf(current));
                    this.editBox.setFilter(s -> s.matches("-?\\d*\\.?\\d*") || s.isEmpty());
                    this.editBox.setResponder(text -> {
                        if (text.isEmpty()) return;
                        try {
                            Object parsed;
                            if (current instanceof Integer) parsed = Integer.parseInt(text);
                            else if (current instanceof Long) parsed = Long.parseLong(text);
                            else if (current instanceof Float) parsed = Float.parseFloat(text);
                            else if (current instanceof Double) parsed = Double.parseDouble(text);
                            else return;

                            Object clamped = ConfigBinder.clampRaw(parsed, this.value);
                            ((ConfigValue<Object>) this.value).set(clamped);
                            ConfigScreen.this.markDirty(this.value);
                        } catch (NumberFormatException ignored) {}
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

                g.drawString(ConfigScreen.this.font, name, left + 4, top + 6, 0xFFFFFFFF, true);

                if (this.unavailable) {
                    g.drawString(ConfigScreen.this.font,
                            Component.translatable("gui.craftcorelib.config.unavailable"),
                            left + width - 130, top + 6, 0xFF888888, false);
                    return;
                }

                if (this.editBox != null) {
                    this.editBox.setX(left + width - 90);
                    this.editBox.setY(top + 3);
                    this.editBox.render(g, mouseX, mouseY, partialTick);
                } else if (this.toggleButton != null) {
                    this.toggleButton.setX(left + width - 60);
                    this.toggleButton.setY(top + 3);
                    this.toggleButton.render(g, mouseX, mouseY, partialTick);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.editBox != null && this.editBox.mouseClicked(mouseX, mouseY, button)) {
                    ConfigScreen.this.focusConfigEditBox(this.editBox);
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
}