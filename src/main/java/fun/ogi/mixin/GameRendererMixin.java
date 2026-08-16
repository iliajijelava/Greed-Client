package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.EventManager;
import fun.ogi.events.render.AspectRatioEvent;
import fun.ogi.module.impl.list.render.Removals;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow private float zoom;

    @Shadow private float zoomX;

    @Shadow
    private float zoomY;

    @Shadow
    public float getFarPlaneDistance() {
        return 0;
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void hurtCamera(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        Removals removals = Cheap.getInstance().getModuleStorage().get(Removals.class);

        if (removals != null && removals.isEnabled("Hurt Camera")) {
            ci.cancel();
        }
    }
    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void floating(ItemStack stack, CallbackInfo ci) {

        Removals removals = Cheap.getInstance().getModuleStorage().get(Removals.class);

        if(removals != null &&removals.isTotemAnimationDisabled()) {
            ci.cancel();
        }
    }
    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatioEvent aspectRatioEvent = new AspectRatioEvent();
        EventManager.call(aspectRatioEvent);
        if (aspectRatioEvent.isCancelled()) {
            Matrix4f matrix4f = new Matrix4f();
            if (zoom != 1.0f) {
                matrix4f.translate(zoomX, -zoomY, 0.0f);
                matrix4f.scale(zoom, zoom, 1.0f);
            }
            matrix4f.perspective(fovDegrees * 0.01745329238474369F, aspectRatioEvent.getRatio(), 0.05f, getFarPlaneDistance());
            cir.setReturnValue(matrix4f);
        }
    }
}