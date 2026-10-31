package net.tearpelato.craftcorelib.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.tearpelato.craftcorelib.api.event.screen.ScreenRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void craftcore$afterBg(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ScreenRenderEvent.AFTER_RENDER_BACKGROUND.post()
                .render((Screen) (Object) this, graphics, mouseX, mouseY,partialTick);
    }
}