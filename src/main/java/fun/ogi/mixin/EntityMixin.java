package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.BoundingBoxControlEvent;
import fun.ogi.events.PushEvent;
import fun.ogi.module.impl.list.player.NoPush;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<Box> cir) {
        Entity self = (Entity) (Object) this;
        if (self == MinecraftClient.getInstance().player) return;

        Box original = cir.getReturnValue();
        BoundingBoxControlEvent event = new BoundingBoxControlEvent(self, original);
        Cheap.getInstance().getEventBus().post(event);

        Box modified = event.getBox();
        if (!modified.equals(original)) {
            cir.setReturnValue(modified);
        }
    }

    @Inject(method = "isPushedByFluids", at = @At("RETURN"), cancellable = true)
    private void onIsPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
        PushEvent event = new PushEvent(PushEvent.Type.WATER);
        Cheap.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("RETURN"), cancellable = true)
    private void onIsPushable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self != MinecraftClient.getInstance().player) return;

        PushEvent event = new PushEvent(PushEvent.Type.COLLISION);
        Cheap.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

}

