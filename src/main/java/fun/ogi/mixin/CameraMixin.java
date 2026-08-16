package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.EventRotation;
import fun.ogi.module.impl.list.movement.FreeCam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
            )
    )
    private void redirectSetRotation(Camera instance, float yaw, float pitch, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
        EventRotation event = new EventRotation(yaw, pitch, tickDelta);
        Cheap.getInstance().getEventBus().post(event);

        float newYaw = event.getYaw();
        float newPitch = event.getPitch();

        if (thirdPerson && inverseView) {
            newYaw += 180.0F;
            newPitch = -newPitch;
        }

        ((ICameraMixin) instance).setCustomRotation(newYaw, newPitch);
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(float desiredCameraDistance, CallbackInfoReturnable<Float> cir) {
        FreeCam freeCam = Cheap.getInstance().getModuleStorage().get(FreeCam.class);
        if (freeCam != null && freeCam.isEnabled()) {
            cir.setReturnValue(desiredCameraDistance);
        }
    }
}

