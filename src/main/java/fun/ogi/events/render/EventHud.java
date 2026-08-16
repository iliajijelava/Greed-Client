package fun.ogi.events.render;

import fun.ogi.events.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class EventHud extends Event {
    private final DrawContext drawContext;
    private final RenderTickCounter renderTickCounter;

    public EventHud(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        this.drawContext = drawContext;
        this.renderTickCounter = renderTickCounter;
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }

    public RenderTickCounter getRenderTickCounter() {
        return renderTickCounter;
    }
}

