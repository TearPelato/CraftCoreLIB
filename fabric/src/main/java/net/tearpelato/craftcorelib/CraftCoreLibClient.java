package net.tearpelato.craftcorelib;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.tearpelato.craftcorelib.api.event.screen.ScreenRenderEvent;
import net.tearpelato.craftcorelib.api.network.NetworkBuilder;
import net.tearpelato.craftcorelib.network.FabricNetworkRegistrar;

public class CraftCoreLibClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenRenderEvent.INIT.post().init(screen);
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {

            ScreenEvents.beforeRender(screen).register((screen1, graphics, mouseX, mouseY, partialTick) -> {
                ScreenRenderEvent.ON_RENDER_BACKGROUND.post().render(screen1, graphics, mouseX, mouseY);
            });

            ScreenMouseEvents.beforeMouseScroll(screen).register((screen1, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
                ScreenRenderEvent.SCROLL.post().onScroll(mouseX, mouseY, verticalAmount);
            });

            ScreenEvents.remove(screen).register(closedScreen -> {
                ScreenRenderEvent.ON_CLOSE.post().close(closedScreen);
            });
        });

        NetworkBuilder.flush(new FabricNetworkRegistrar());
    }
}
