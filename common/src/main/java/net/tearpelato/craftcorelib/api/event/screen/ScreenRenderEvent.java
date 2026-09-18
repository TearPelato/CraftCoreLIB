package net.tearpelato.craftcorelib.api.event.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.tearpelato.craftcorelib.api.event.Event;

public class ScreenRenderEvent {

    public static final Event<Init> INIT = new Event<>(Init.class);
    public static final Event<BackgroundRender> ON_RENDER_BACKGROUND = new Event<>(BackgroundRender.class);
    public static final Event<Scroll> SCROLL = new Event<>(Scroll.class);
    public static final Event<Close> ON_CLOSE = new Event<>(Close.class);


    public interface Init {
        void init(Screen screen);
    }

    public interface BackgroundRender {
        void render(Screen screen, GuiGraphics graphics, int x, int y);

    }

    public interface Scroll {
        void onScroll(double x, double y, double z);
    }


    public interface Close {
        void close(Screen screen);
    }

}
