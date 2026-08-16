package fun.ogi.events.render;

import fun.ogi.events.Event;
import net.minecraft.entity.Entity;

public class EventEntityColor extends Event {
    private final Entity entity;
    private int color = -1;

    public EventEntityColor(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}

