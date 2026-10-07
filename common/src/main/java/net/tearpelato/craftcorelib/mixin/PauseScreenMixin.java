package net.tearpelato.craftcorelib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.tearpelato.craftcorelib.CraftCoreLIBConstants;
import net.tearpelato.craftcorelib.api.config.screen.ConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    @Unique
    private AbstractWidget craftcorelib$configButton;

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "createPauseMenu", at = @At("RETURN"))
    private void craftcorelib$addConfigButton(CallbackInfo ci) {
        AbstractWidget returnToGame = craftcorelib$findReturnToGameButton();
        if (returnToGame == null) return;

        Identifier icon = Identifier.fromNamespaceAndPath(CraftCoreLIBConstants.MOD_ID, "config_icon");
        Identifier iconHighlighted = Identifier.fromNamespaceAndPath(CraftCoreLIBConstants.MOD_ID, "config_icon_highlighted");
        WidgetSprites sprites = new WidgetSprites(icon, iconHighlighted);

        int buttonSize = 20;
        int spacing = 4;

        int x = returnToGame.getX() + returnToGame.getWidth() + spacing;
        int by = returnToGame.getY() + (returnToGame.getHeight() - buttonSize) / 2;

        this.craftcorelib$configButton = new ImageButton(
                x, by, buttonSize, buttonSize,
                sprites,
                button -> Minecraft.getInstance().setScreen(new ConfigScreen(this)),
                Component.translatable("gui.craftcorelib.config.title")
        );

        this.addRenderableWidget(this.craftcorelib$configButton);
    }

    @Unique
    private AbstractWidget craftcorelib$findReturnToGameButton() {
        String target = Component.translatable("menu.returnToGame").getString();
        for (var child : this.children()) {
            if (child instanceof AbstractWidget widget) {
                if (widget.getMessage().getString().equals(target)) {
                    return widget;
                }
            }
        }
        return null;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void craftcorelib$keepAligned(CallbackInfo ci) {
        if (this.craftcorelib$configButton == null) return;

        AbstractWidget returnToGame = craftcorelib$findReturnToGameButton();
        if (returnToGame != null) {
            this.craftcorelib$configButton.setX(returnToGame.getX() + returnToGame.getWidth() + 4);
            this.craftcorelib$configButton.setY(
                    returnToGame.getY() + (returnToGame.getHeight() - this.craftcorelib$configButton.getHeight()) / 2
            );
        }
    }
}