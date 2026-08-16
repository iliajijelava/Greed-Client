package fun.ogi.events.render;

import fun.ogi.events.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class EventHandledScreenRender extends Event {

    private final HandledScreen screen;
    private final DrawContext drawContext;

    public EventHandledScreenRender(HandledScreen screen, DrawContext drawContext) {
        this.screen = screen;
        this.drawContext = drawContext;
    }

    public HandledScreen getScreen() {
        return screen;
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }
}

