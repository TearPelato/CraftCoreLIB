package net.tearpelato.craftcorelib.api.config.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

public class ConfigScreen extends Screen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private record Target(String modId, ConfigType type) {}
    private record Icon(Identifier location, int width, int height) {}

    private static final Icon FALLBACK_ICON = new Icon(Identifier.withDefaultNamespace("textures/misc/unknown_pack.png"), 64, 64);

    private final Screen parent;

    private final Map<ConfigValue<?>, Target> dirty = new HashMap<>();
    private final Map<ConfigValue<?>, Object> originalValues = new HashMap<>();
    private List<ConfigValue<?>> visibleValues = List.of();

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
    private static boolean externalScanned = false;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("gui.craftcorelib.config.title"));
        this.parent = parent;
    }


    @Override
    protected void init() {
        super.init();

        if (!externalScanned) {
            Services.CONFIG.scanExternalConfigs();
            externalScanned = true;
        }

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
        Set<String> all = new TreeSet<>(
                Comparator.comparing((String id) -> Services.PLATFORM.getModName(id).toLowerCase(Locale.ROOT))
                        .thenComparing(Comparator.naturalOrder()));

        all.addAll(ConfigManager.getRegisteredModIds());
        return all;
    }

    @Override
    public void onClose() {
        if (!this.dirty.isEmpty()) {
            this.minecraft.gui.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            saveChanges();
                        } else {
                            revertChanges();
                        }
                        this.minecraft.gui.setScreen(this.parent);
                    },
                    Component.translatable("gui.craftcorelib.config.unsaved_title"),
                    Component.translatable("gui.craftcorelib.config.unsaved_message"),
                    Component.translatable("gui.craftcorelib.config.save"),
                    Component.translatable("gui.craftcorelib.config.discard")
            ));
            return;
        }
        this.minecraft.gui.setScreen(this.parent);
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
        if (modId.equals(this.selectedModId)) {
            this.selectedModId = null;
            this.selectedType = null;
        } else {
            this.selectedModId = modId;
            this.selectedType = null;
        }
        updateRightPanel();
    }

    private void updateRightPanel() {
        this.typeButtons.forEach(this::removeWidget);
        this.typeButtons.clear();

        if (this.resetButton != null) {
            this.removeWidget(this.resetButton);
            this.resetButton = null;
        }
        if (this.saveButton != null) {
            this.removeWidget(this.saveButton);
            this.saveButton = null;
        }

        if (this.selectedModId == null) {
            return;
        }

        List<ConfigCategory> categories = ConfigManager.getCategories(this.selectedModId);
        if (categories.isEmpty()) {
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

        int btnWidth = 200;
        int btnHeight = 20;
        int gap = 8;

        int rightX = LEFT_PANEL_WIDTH + (this.width - LEFT_PANEL_WIDTH - btnWidth) / 2;
        int y = TOP_BAR_HEIGHT + 55;

        if (hasClient) {
            Button btn = createBigTypeButton(ConfigType.CLIENT, rightX, y, btnWidth, btnHeight, true, null);
            this.typeButtons.add(btn);
            this.addRenderableWidget(btn);
            y += btnHeight + gap;
        }
        if (hasServer) {
            boolean available = this.minecraft.level != null;
            Component tooltip = available ? null
                    : Component.translatable("gui.craftcorelib.config.server_unavailable");
            Button btn = createBigTypeButton(ConfigType.SERVER, rightX, y, btnWidth, btnHeight, available, tooltip);
            this.typeButtons.add(btn);
            this.addRenderableWidget(btn);
            y += btnHeight + gap;
        }
        if (hasCommon) {
            Button btn = createBigTypeButton(ConfigType.COMMON, rightX, y, btnWidth, btnHeight, true, null);
            this.typeButtons.add(btn);
            this.addRenderableWidget(btn);
        }
    }

    private Button createBigTypeButton(ConfigType type, int x, int y, int w, int h,
                                       boolean enabled, Component disabledTooltip) {
        Component label = Component.translatable("gui.craftcorelib.config.type." + type.name().toLowerCase(Locale.ROOT));
        Button btn = Button.builder(label, b -> openTypeScreen(type))
                .bounds(x, y, w, h)
                .build();
        btn.active = enabled;
        if (!enabled && disabledTooltip != null) {
            btn.setTooltip(Tooltip.create(disabledTooltip));
        }
        return btn;
    }

    private void openTypeScreen(ConfigType type) {
        this.minecraft.gui.setScreen(new ConfigTypeScreen(this, this.selectedModId, type));
    }

    private void focusConfigEditBox(EditBox box) {
        if (this.activeConfigEditBox != null && this.activeConfigEditBox != box) {
            this.activeConfigEditBox.setFocused(false);
        }
        this.activeConfigEditBox = box;
        if (box != null) box.setFocused(true);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.activeConfigEditBox != null && this.getFocused() == this.entryList
                && this.activeConfigEditBox.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.activeConfigEditBox != null && this.getFocused() == this.entryList
                && this.activeConfigEditBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }


    private Icon resolveIcon(String modId) {
        return this.iconCache.computeIfAbsent(modId, id -> {
            Identifier location = null;
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


    private Icon probeIcon(Identifier identifier) {
        AbstractTexture registered = this.minecraft.getTextureManager().getTexture(identifier);
        if (registered instanceof DynamicTexture dyn && dyn.getPixels() != null) {
            return new Icon(identifier, dyn.getPixels().getWidth(), dyn.getPixels().getHeight());
        }

        Optional<Resource> resource = this.minecraft.getResourceManager().getResource(identifier);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                return new Icon(identifier, image.getWidth(), image.getHeight());
            } catch (IOException ignored) {}
        }
        return null;
    }

    private void drawIcon(GuiGraphicsExtractor g, String modId, int x, int y, int size) {
        Icon icon = resolveIcon(modId);
        g.blit(RenderPipelines.GUI_TEXTURED, icon.location(), x, y, 0f, 0f,
                size, size, icon.width(), icon.height(), icon.width(), icon.height());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(this.font, this.title, 10, 10, 0xFFFFFF, true);

        if (this.selectedModId != null) {
            int logoX = LEFT_PANEL_WIDTH + 16;
            int logoY = TOP_BAR_HEIGHT + 10;
            int logoSize = 32;

            drawIcon(graphics, this.selectedModId, logoX, logoY, logoSize);

            String displayName = Services.PLATFORM.getModName(this.selectedModId);
            graphics.text(this.font, displayName,
                    logoX + logoSize + 8, logoY + 10,
                    0xFFFFFFFF, true);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, a);
        } else {
            this.extractTransparentBackground(graphics);
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
            public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float partialTick) {
                boolean selected = this.modId.equals(ConfigScreen.this.selectedModId);

                int x = getContentX();
                int y = getContentY();
                int w = getContentWidth();
                int h = getContentHeight();

                if (selected) {
                    g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x80FFFFFF);
                } else if (hovered) {
                    g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x40FFFFFF);
                }

                ConfigScreen.this.drawIcon(g, this.modId, x + 2, y + 2, 20);
                g.text(ConfigScreen.this.font, this.displayName, x + 26, y + (h - 8) / 2, 0xFFFFFFFF, true);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
                    this.editBox = new EditBox(ConfigScreen.this.font, 0, 0, 80, 18, Component.empty()){
                        @Override
                        public void insertText(String input) {
                            String old = getValue();
                            int cursor = getCursorPosition();
                            super.insertText(input);
                            if (!getValue().matches("-?\\d*\\.?\\d*")) {
                                setValue(old);
                                setCursorPosition(cursor);
                                setHighlightPos(cursor);
                            }
                        }
                    };
                    this.editBox.setValue(String.valueOf(current));
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
            public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float partialTick) {
                int x = getContentX();
                int y = getContentY();
                int w = getContentWidth();

                String nameKey = this.value.getNameKey();
                Component name = nameKey != null
                        ? Component.translatable(nameKey)
                        : Component.literal(this.value.getKey());
                g.text(ConfigScreen.this.font, name, x + 4, y + 6, 0xFFFFFFFF, true);

                if (this.unavailable) {
                    g.text(ConfigScreen.this.font,
                            Component.translatable("gui.craftcorelib.config.unavailable"),
                            x + w - 130, y + 6, 0xFF888888, false);
                    return;
                }

                if (this.editBox != null) {
                    this.editBox.setX(x + w - 90);
                    this.editBox.setY(y + 3);
                    this.editBox.extractRenderState(g, mouseX, mouseY, partialTick);
                } else if (this.toggleButton != null) {
                    this.toggleButton.setX(x + w - 60);
                    this.toggleButton.setY(y + 3);
                    this.toggleButton.extractRenderState(g, mouseX, mouseY, partialTick);
                }
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean mouseClicked) {
                if (this.editBox != null && this.editBox.mouseClicked(event, mouseClicked)) {
                    ConfigScreen.this.focusConfigEditBox(this.editBox);
                    return true;
                }
                if (this.toggleButton != null && this.toggleButton.mouseClicked(event, mouseClicked)) {
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