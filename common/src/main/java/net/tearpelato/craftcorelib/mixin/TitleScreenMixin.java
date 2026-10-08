package net.tearpelato.craftcorelib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.tearpelato.craftcorelib.CraftCoreLIBConstants;
import net.tearpelato.craftcorelib.api.config.screen.ConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Unique
    private AbstractWidget craftcorelib$configButton;

    @Inject(method = "createNormalMenuOptions", at = @At("RETURN"))
    private void craftcorelib$addConfigButton(int topPos, int spacing, CallbackInfoReturnable<Integer> cir) {
        TitleScreen self = (TitleScreen) (Object) this;

        AbstractWidget singleplayerButton = craftcorelib$findButton(self);
        if (singleplayerButton == null) return;

        Identifier icon = Identifier.fromNamespaceAndPath(CraftCoreLIBConstants.MOD_ID, "config_icon");
        Identifier iconHighlighted = Identifier.fromNamespaceAndPath(CraftCoreLIBConstants.MOD_ID, "config_icon_highlighted");
        WidgetSprites sprites = new WidgetSprites(icon, iconHighlighted);

        int buttonSize = 20;
        int spacings = 4;

        int x = singleplayerButton.getX() + singleplayerButton.getWidth() + spacings;
        int by = singleplayerButton.getY() + (singleplayerButton.getHeight() - buttonSize) / 2;

        this.craftcorelib$configButton = new ImageButton(
                x, by, buttonSize, buttonSize,
                sprites,
                button -> Minecraft.getInstance().gui.setScreen(new ConfigScreen(self)),
                Component.translatable("gui.craftcorelib.config.title")
        );

        ((ScreenAccessor) self).callAddRenderableWidget(this.craftcorelib$configButton);
    }

    @Unique
    private static AbstractWidget craftcorelib$findButton(TitleScreen self) {
        String target = Component.translatable("menu.singleplayer").getString();
        for (var child : ((ScreenAccessor) self).callChildren()) {
            if (child instanceof AbstractWidget widget) {
                Component msg = widget.getMessage();
                if (msg.getString().equals(target)) {
                    return widget;
                }
            }
        }
        return null;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void craftcorelib$keepAligned(CallbackInfo ci) {
        if (this.craftcorelib$configButton == null) return;

        TitleScreen self = (TitleScreen) (Object) this;
        AbstractWidget singleplayerButton = craftcorelib$findButton(self);

        if (singleplayerButton != null) {
            this.craftcorelib$configButton.setX(singleplayerButton.getX() + singleplayerButton.getWidth() + 4);
            this.craftcorelib$configButton.setY(singleplayerButton.getY()
                    + (singleplayerButton.getHeight() - this.craftcorelib$configButton.getHeight()) / 2);
        }
    }
}
