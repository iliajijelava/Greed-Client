package fun.ogi.events.player;


import fun.ogi.events.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;



public class EventAttackEntity extends Event {
    private final PlayerEntity player;
    private final Entity target;

    public EventAttackEntity(PlayerEntity player, Entity target) {
        this.player = player;
        this.target = target;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Entity getTarget() {
        return target;
    }
}