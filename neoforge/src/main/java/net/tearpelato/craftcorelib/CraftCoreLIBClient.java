package net.tearpelato.craftcorelib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.tearpelato.craftcorelib.api.event.screen.ScreenRenderEvent;
import net.tearpelato.craftcorelib.api.network.Network;
import net.tearpelato.craftcorelib.api.network.NetworkBuilder;
import net.tearpelato.craftcorelib.network.NeoForgeNetworkRegistrar;
import net.tearpelato.craftcorelib.network.NeoForgeNetworkSender;

@EventBusSubscriber(modid = CraftCoreLIBConstants.MOD_ID, value = Dist.CLIENT)
public class CraftCoreLIBClient {

    @SubscribeEvent
    public static void onContainerInit(ScreenEvent.Init.Post event) {
        ScreenRenderEvent.INIT.post().init(event.getScreen());
    }

    @SubscribeEvent
    public static void onContainerRender(ScreenEvent.Render.Background event) {
        ScreenRenderEvent.ON_RENDER_BACKGROUND.post().render(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void onContainerClose(ScreenEvent.Closing event) {
        ScreenRenderEvent.ON_CLOSE.post().close(event.getScreen());
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        ScreenRenderEvent.SCROLL.post().onScroll(event.getMouseX(), event.getMouseY(),event.getScrollDeltaY());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        Network.setSender(new NeoForgeNetworkSender());
        var registrar = new NeoForgeNetworkRegistrar(event.registrar(NetworkBuilder.NETWORK_VERSION));
        NetworkBuilder.flush(registrar);
    }


}
