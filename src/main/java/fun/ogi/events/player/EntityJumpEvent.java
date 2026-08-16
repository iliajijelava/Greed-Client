package fun.ogi.events.player;


import fun.ogi.events.EventCancellable;
import net.minecraft.entity.LivingEntity;

public class EntityJumpEvent extends EventCancellable {
   private final LivingEntity entity;


   public LivingEntity getEntity() {
      return this.entity;
   }

   public EntityJumpEvent(LivingEntity entity) {
      this.entity = entity;
   }
}

