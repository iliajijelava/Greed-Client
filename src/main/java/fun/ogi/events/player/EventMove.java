package fun.ogi.events.player;


import fun.ogi.events.Event;
import net.minecraft.util.math.Vec3d;

public class EventMove extends Event {
   private Vec3d movePos;

   public EventMove(Vec3d movePos) {
      this.movePos = movePos;
   }

   public Vec3d getMovePos() {
      return movePos;
   }
}