package net.tearpelato.craftcorelib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tearpelato.craftcorelib.Constants;
import net.tearpelato.craftcorelib.api.config.screen.ConfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Unique
    private AbstractWidget craftcorelib$configButton;

    @Inject(method = "createNormalMenuOptions", at = @At("RETURN"))
    private void craftcorelib$addConfigButton(int y, int rowHeight, CallbackInfo ci) {
        TitleScreen self = (TitleScreen) (Object) this;

        AbstractWidget realmsButton = null;
        for (var child : ((ScreenAccessor) self).callChildren()) {
            if (child instanceof AbstractWidget widget) {
                Component msg = widget.getMessage();
                if (msg != null && msg.getString().equals(Component.translatable("menu.online").getString())) {
                    realmsButton = widget;
                    break;
                }
            }
        }

        if (realmsButton == null) {
            return;
        }

        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/config_icon.png");
        WidgetSprites sprites = new WidgetSprites(icon, icon);

        int buttonSize = 20;
        int spacing = 4;

        this.craftcorelib$configButton = new ImageButton(
                realmsButton.getX() + realmsButton.getWidth() + spacing,
                realmsButton.getY(),
                buttonSize,
                buttonSize,
                sprites,
                button -> Minecraft.getInstance().setScreen(new ConfigScreen(self)),
                Component.translatable("gui.craftcorelib.config.title")
        );

        ((ScreenAccessor) self).callAddRenderableWidget(this.craftcorelib$configButton);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void craftcorelib$keepAligned(CallbackInfo ci) {
        if (this.craftcorelib$configButton == null) return;

        TitleScreen self = (TitleScreen) (Object) this;
        AbstractWidget realmsButton = null;

        for (var child : self.children()) {
            if (child instanceof AbstractWidget widget) {
                Component msg = widget.getMessage();
                if (msg != null && msg.getString().equals(Component.translatable("menu.online").getString())) {
                    realmsButton = widget;
                    break;
                }
            }
        }

        if (realmsButton != null) {
            this.craftcorelib$configButton.setX(realmsButton.getX() + realmsButton.getWidth() + 4);
            this.craftcorelib$configButton.setY(realmsButton.getY());
        }
    }
}
