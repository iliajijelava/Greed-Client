package fun.ogi.events;

import net.minecraft.entity.Entity;

public class AttackEvent extends Event {
    private final Entity entity;

    public AttackEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}

