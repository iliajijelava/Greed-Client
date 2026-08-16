package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.render.EventSwingDuration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        EventSwingDuration event = new EventSwingDuration((float) cir.getReturnValueI());
        Cheap.getInstance().getEventBus().post(event);
        cir.setReturnValue(Math.round(event.getDuration()));
    }
}

