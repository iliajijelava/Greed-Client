package fun.ogi.util.rotation;

import com.google.common.eventbus.Subscribe;

import fun.ogi.events.EventLook;
import fun.ogi.events.EventRotation;
import net.minecraft.util.math.MathHelper;


public class FreeLookComponent   {

    private static final FreeLookComponent INSTANCE = new FreeLookComponent();

    private static boolean active;

    private static float freeYaw, freePitch;

    public static FreeLookComponent getInstance() {
        return INSTANCE;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getFreeYaw(){
        return freeYaw;
    }
    public static float getFreePitch(){
        return freePitch;
    }
    public static void setActive(boolean active){
        FreeLookComponent.active = active;
    }
    @Subscribe
    public void onEvent(EventLook event) {
        if (active) {
            rotateTowards(event.getYaw(), event.getPitch());
            event.cancelEvent();
        }
    }

    @Subscribe
    public void onEvent(EventRotation event) {
        if (active) {
            event.setYaw(freeYaw);
            event.setPitch(freePitch);
        } else {
            freeYaw = event.getYaw();
            freePitch = event.getPitch();
        }
    }

    private void rotateTowards(double targetYaw, double targetPitch) {
        freePitch = MathHelper.clamp((float) (freePitch + targetPitch * 0.15D), -90.0F, 90.0F);
        freeYaw = (float) (freeYaw + targetYaw * 0.15D);
    }
}