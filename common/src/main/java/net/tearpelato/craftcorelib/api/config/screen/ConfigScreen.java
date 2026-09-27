package net.tearpelato.craftcorelib.api.config.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tearpelato.craftcorelib.Constants;
import net.tearpelato.craftcorelib.api.config.ConfigCategory;
import net.tearpelato.craftcorelib.api.config.ConfigManager;
import net.tearpelato.craftcorelib.api.config.ConfigType;
import net.tearpelato.craftcorelib.api.config.ConfigValue;
import net.tearpelato.craftcorelib.platform.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConfigScreen extends Screen {

    private final Screen parent;

    private static final int LEFT_PANEL_WIDTH = 140;
    private static final int TOP_BAR_HEIGHT = 32;

    private static final ResourceLocation LEFT_BG  = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/config/left_panel.png");
    private static final ResourceLocation RIGHT_BG = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/config/right_panel.png");

    private EditBox searchBox;
    private ModListWidget modList;

    private String selectedModId = null;
    private ConfigType selectedType = null;

    private final List<Button> typeButtons = new ArrayList<>();
    private ConfigEntryList entryList;

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

        this.modList.refresh(ConfigManager.getRegisteredModIds());

        updateRightPanel();
    }

    private void filterMods(String query) {
        this.modList.filter(query);
    }

    /** Toggle selezione mod */
    public void selectMod(String modId) {
        if (modId.equals(this.selectedModId)) {
            // Riclick → deseleziona
            this.selectedModId = null;
            this.selectedType = null;
        } else {
            this.selectedModId = modId;
            this.selectedType = null;
        }
        updateRightPanel();
    }

    /** Toggle selezione tipo (CLIENT / SERVER / COMMON) */
    public void selectType(ConfigType type) {
        if (type == this.selectedType) {
            // Riclick → deseleziona
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

        if (this.selectedModId == null) return;

        List<ConfigCategory> categories = ConfigManager.getCategories(this.selectedModId);
        if (categories.isEmpty()) return;

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
            Button btn = createTypeButton(ConfigType.CLIENT, startX, y, btnWidth, btnHeight);
            this.typeButtons.add(btn);
            this.addRenderableWidget(btn);
            startX += btnWidth + gap;
        }
        if (hasServer) {
            Button btn = createTypeButton(ConfigType.SERVER, startX, y, btnWidth, btnHeight);
            this.typeButtons.add(btn);
            this.addRenderableWidget(btn);
            startX += btnWidth + gap;
        }
        if (hasCommon) {
            Button btn = createTypeButton(ConfigType.COMMON, startX, y, btnWidth, btnHeight);
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

            int listY = y + btnHeight + 12;
            this.entryList = new ConfigEntryList(this.minecraft,
                    this.width - LEFT_PANEL_WIDTH - 40,
                    this.height - listY - 16,
                    listY,
                    26);
            this.entryList.setX(rightX);
            this.entryList.setEntries(values);
            this.addRenderableWidget(this.entryList);
        }
    }

    private Button createTypeButton(ConfigType type, int x, int y, int w, int h) {
        // Evidenzia il bottone se è quello attualmente selezionato
        Component label = Component.literal(type.name());
        return Button.builder(label, btn -> selectType(type))
                .bounds(x, y, w, h)
                .build();
    }

    // =========================================================
    // RENDERING – Niente blur
    // =========================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background vanilla senza blur
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // Pannelli con texture
        graphics.blit(LEFT_BG, 0, TOP_BAR_HEIGHT, 0, 0,
                LEFT_PANEL_WIDTH, this.height - TOP_BAR_HEIGHT,
                LEFT_PANEL_WIDTH, this.height - TOP_BAR_HEIGHT);

        graphics.blit(RIGHT_BG, LEFT_PANEL_WIDTH, TOP_BAR_HEIGHT, 0, 0,
                this.width - LEFT_PANEL_WIDTH, this.height - TOP_BAR_HEIGHT,
                this.width - LEFT_PANEL_WIDTH, this.height - TOP_BAR_HEIGHT);

        // Titolo
        graphics.drawString(this.font, this.title, 10, 10, 0xFF000000, false);

        // Logo + nome visuale della mod selezionata
        if (this.selectedModId != null) {
            int logoX = LEFT_PANEL_WIDTH + 16;
            int logoY = TOP_BAR_HEIGHT + 10;
            int logoSize = 32;

            ResourceLocation logo = Services.PLATFORM.getModIcon(this.selectedModId);
            if (logo != null) {
                graphics.blit(logo, logoX, logoY, 0, 0, logoSize, logoSize, logoSize, logoSize);
            }

            String displayName = Services.PLATFORM.getModName(this.selectedModId);
            graphics.drawString(this.font, displayName,
                    logoX + logoSize + 8, logoY + 10,
                    0xFFFFFFFF, true);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Completamente senza blur
        if (this.minecraft.level != null) {
            graphics.fill(0, 0, this.width, this.height, 0xC0101010);
        } else {
            // Menu principale → lascia il panorama vanilla
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    // =========================================================
    // LISTA MOD
    // =========================================================

    public class ModListWidget extends ObjectSelectionList<ModListWidget.Entry> {

        private final List<String> allMods = new ArrayList<>();

        public ModListWidget(Minecraft mc, int width, int height, int y, int itemHeight) {
            super(mc, width, height, y, itemHeight);
        }

        public void refresh(Set<String> modIds) {
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

                if (selected) {
                    g.fill(left, top, left + width, top + height, 0x80FFFFFF);
                } else if (hovering) {
                    g.fill(left, top, left + width, top + height, 0x40FFFFFF);
                }

                ResourceLocation icon = Services.PLATFORM.getModIcon(this.modId);
                if (icon != null) {
                    g.blit(icon, left + 4, top + 4, 0, 0, 16, 16, 16, 16);
                }

                g.drawString(ConfigScreen.this.font, this.displayName,
                        left + 24, top + 8, 0xFF000000, false);
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

    // =========================================================
    // LISTA ENTRY
    // =========================================================

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

            @SuppressWarnings("unchecked")
            public Entry(ConfigValue<?> value) {
                this.value = value;
                Object current = value.get();

                if (current instanceof Boolean) {
                    boolean bool = (Boolean) current;
                    this.toggleButton = Button.builder(
                            Component.literal(bool ? "ON" : "OFF"),
                            btn -> {
                                boolean next = !(Boolean) this.value.get();
                                ((ConfigValue<Boolean>) this.value).set(next);
                                btn.setMessage(Component.literal(next ? "ON" : "OFF"));
                            }
                    ).size(50, 18).build();
                } else if (current instanceof Number) {
                    this.editBox = new EditBox(ConfigScreen.this.font, 0, 0, 80, 18, Component.empty());
                    this.editBox.setValue(String.valueOf(current));
                    this.editBox.setFilter(s -> s.matches("-?\\d*\\.?\\d*") || s.isEmpty());
                    this.editBox.setResponder(text -> {
                        if (text.isEmpty()) return;
                        try {
                            if (current instanceof Integer) {
                                ((ConfigValue<Integer>) this.value).set(Integer.parseInt(text));
                            } else if (current instanceof Long) {
                                ((ConfigValue<Long>) this.value).set(Long.parseLong(text));
                            } else if (current instanceof Float) {
                                ((ConfigValue<Float>) this.value).set(Float.parseFloat(text));
                            } else if (current instanceof Double) {
                                ((ConfigValue<Double>) this.value).set(Double.parseDouble(text));
                            }
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
                    ConfigScreen.this.setFocused(this.editBox);
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