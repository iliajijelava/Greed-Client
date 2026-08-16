package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(moduleName = "Speed", moduleDesc = "Grim bypass speed", moduleCategory = ModuleCategory.MOVEMENT)
public class Speed extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Grim", "Grim", "Strafe", "Ground");
    private final SliderSetting speed = new SliderSetting("Speed", this, 0.5, 0.1, 2.0, 0.1);

    private int ticks;

    public Speed() {
        addSettings(mode, speed);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ticks = 0;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isGliding()) return;

        switch (mode.getValue()) {
            case "Grim" -> handleGrim();
            case "Strafe" -> handleStrafe();
            case "Ground" -> handleGround();
        }
    }

    private void handleGrim() {
        if (mc.player.forwardSpeed <= 0 && mc.player.sidewaysSpeed == 0) return;
        ticks++;

        if (mc.player.isOnGround() && ticks > 5) {
            mc.player.jump();
            ticks = 0;
        }

        Vec3d vel = mc.player.getVelocity();
        double baseSpeed = speed.getValue() * 0.28;
        float f = mc.player.getYaw() * 0.017453292F;
        Vec3d direction = new Vec3d(
                -MathHelper.sin(f) * 0.02,
                0,
                MathHelper.cos(f) * 0.02
        );

        mc.player.setVelocity(
                vel.x + direction.x * baseSpeed,
                mc.player.isOnGround() ? 0.42 : vel.y,
                vel.z + direction.z * baseSpeed
        );
    }

    private void handleStrafe() {
        if (mc.player.forwardSpeed <= 0 && mc.player.sidewaysSpeed == 0) return;

        Vec3d vel = mc.player.getVelocity();
        double baseSpeed = speed.getValue() * 0.36;

        float yaw = mc.player.getYaw();
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;

        if (forward == 0 && strafe == 0) return;

        if (forward != 0) {
            if (strafe > 0) {
                yaw += forward > 0 ? -45 : 45;
            } else if (strafe < 0) {
                yaw += forward > 0 ? 45 : -45;
            }
            strafe = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90);
        double motionX = forward * baseSpeed * Math.cos(rad) + strafe * baseSpeed * Math.sin(rad);
        double motionZ = forward * baseSpeed * Math.sin(rad) - strafe * baseSpeed * Math.cos(rad);

        if (mc.player.isOnGround()) {
            mc.player.jump();
        }

        mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);

        if (mc.player.isOnGround()) {
            mc.player.setVelocity(motionX, 0.42, motionZ);
        }
    }

    private void handleGround() {
        if (mc.player.forwardSpeed <= 0 && mc.player.sidewaysSpeed == 0) return;
        if (!mc.player.isOnGround()) return;

        Vec3d vel = mc.player.getVelocity();
        double baseSpeed = speed.getValue() * 0.32;

        float yaw = mc.player.getYaw();
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;

        if (forward != 0) {
            if (strafe > 0) {
                yaw += forward > 0 ? -45 : 45;
            } else if (strafe < 0) {
                yaw += forward > 0 ? 45 : -45;
            }
            strafe = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90);
        double motionX = forward * baseSpeed * Math.cos(rad) + strafe * baseSpeed * Math.sin(rad);
        double motionZ = forward * baseSpeed * Math.sin(rad) - strafe * baseSpeed * Math.cos(rad);

        mc.player.setVelocity(motionX, vel.y, motionZ);
    }
}

