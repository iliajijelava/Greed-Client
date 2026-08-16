package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.EventLook;
import fun.ogi.events.EventMouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.util.math.Smoother;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Shadow @Final
    private MinecraftClient client;
    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;
    @Shadow private Smoother cursorXSmoother;
    @Shadow private Smoother cursorYSmoother;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (client.currentScreen == null) return;

        double scaleX = (double) client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double scaleY = (double) client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        double mouseX = client.mouse.getX() * scaleX;
        double mouseY = client.mouse.getY() * scaleY;

        Cheap.getInstance().getEventBus().post(new EventMouse(mouseX, mouseY, button, action));
    }

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
        if (client.player == null) return;

        double sensitivity = client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double scaled = sensitivity * sensitivity * sensitivity * 8.0;
        double i, j;

        if (client.options.smoothCameraEnabled) {
            i = cursorXSmoother.smooth(cursorDeltaX * scaled, timeDelta * scaled);
            j = cursorYSmoother.smooth(cursorDeltaY * scaled, timeDelta * scaled);
        } else if (client.options.getPerspective().isFirstPerson() && client.player.isUsingSpyglass()) {
            cursorXSmoother.clear();
            cursorYSmoother.clear();
            i = cursorDeltaX * sensitivity * sensitivity * sensitivity;
            j = cursorDeltaY * sensitivity * sensitivity * sensitivity;
        } else {
            cursorXSmoother.clear();
            cursorYSmoother.clear();
            i = cursorDeltaX * scaled;
            j = cursorDeltaY * scaled;
        }

        int invert = client.options.getInvertYMouse().getValue() ? -1 : 1;

        EventLook event = new EventLook(i, j * invert);
        Cheap.getInstance().getEventBus().post(event);

        if (!event.isCancelled()) {
            client.getTutorialManager().onUpdateMouse(event.getYaw(), event.getPitch());
            client.player.changeLookDirection(event.getYaw(), event.getPitch());
        }

        cursorDeltaX = 0.0;
        cursorDeltaY = 0.0;

        ci.cancel();
    }
}

