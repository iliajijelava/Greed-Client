package fun.ogi.events;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

public class BoundingBoxControlEvent extends Event {
    private final Entity entity;
    private Box box;

    public BoundingBoxControlEvent(Entity entity, Box box) {
        this.entity = entity;
        this.box = box;
    }

    public Entity getEntity() {
        return entity;
    }

    public Box getBox() {
        return box;
    }

    public void setBox(Box box) {
        this.box = box;
    }
}

