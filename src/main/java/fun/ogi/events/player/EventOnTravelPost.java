package fun.ogi.events.player;

import fun.ogi.events.Event;
import net.minecraft.util.math.Vec3d;

public class EventOnTravelPost extends Event {
    private final Vec3d oldVelocity;

    public EventOnTravelPost(Vec3d oldVelocity) {
        this.oldVelocity = oldVelocity;
    }

    public Vec3d getOldVelocity() {
        return oldVelocity;
    }
}

