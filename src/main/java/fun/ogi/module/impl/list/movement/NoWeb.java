package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ModeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.*;

import java.util.Iterator;

import static fun.ogi.util.MinecraftUtil.mc;

@ModuleInformation(moduleName = "No Web", moduleDesc = "Removes slowness from cobweb", moduleCategory = ModuleCategory.MOVEMENT)
public class NoWeb extends Module {

    public final ModeSetting mode = new ModeSetting("Mode", this, "Collision",
            "Collision", "Motion", "Strafe");

    public NoWeb() {
        addSettings(mode);
    }

    @Subscribe
    public void onUpdate(final EventUpdate eventUpdate) {
        if (mc.player == null || mc.world == null) return;

        switch (mode.getValue()) {
            case "Collision" -> handleCollision();
            case "Motion" -> handleMotion();
            case "Strafe" -> handleStrafe();
        }
    }

    private void handleCollision() {
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
                    }
                }
            }
        }
    }

    private boolean isInWeb() {
        Box box = mc.player.getBoundingBox();
        Iterator<BlockPos> it = BlockPos.iterate(
                MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ),
                MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ)
        ).iterator();

        while (it.hasNext()) {
            if (mc.world.getBlockState(it.next()).isOf(Blocks.COBWEB)) return true;
        }
        return false;
    }

    private void handleMotion() {
        if (!isInWeb()) return;

        Vec3d vel = mc.player.getVelocity();

        if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(vel.x, 0.8, vel.z);
        } else if (mc.options.sneakKey.isPressed()) {
            mc.player.setVelocity(vel.x, -0.8, vel.z);
        } else {
            mc.player.setVelocity(vel.x, 0.0, vel.z);
        }

        float yaw = mc.player.getYaw();
        double forward = mc.player.input.movementForward;
        double strafe = mc.player.input.movementSideways;

        if (forward == 0 && strafe == 0) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            return;
        }

        if (forward != 0) {
            if (strafe > 0) {
                yaw += forward > 0 ? -45 : 45;
            } else if (strafe < 0) {
                yaw += forward > 0 ? 45 : -45;
            }
            strafe = 0;
            forward = forward > 0 ? 1 : -1;
        }

        double speed = 0.21;
        double rad = Math.toRadians(yaw + 90);
        double motionX = forward * speed * Math.cos(rad) + strafe * speed * Math.sin(rad);
        double motionZ = forward * speed * Math.sin(rad) - strafe * speed * Math.cos(rad);

        mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
    }

    private void handleStrafe() {
        if (!isInWeb()) return;

        Vec3d vel = mc.player.getVelocity();
        float yaw = mc.player.getYaw();
        double forward = 0;
        double strafe = 0;

        if (mc.player.input.playerInput.forward()) forward++;
        if (mc.player.input.playerInput.backward()) forward--;
        if (mc.player.input.playerInput.left()) strafe++;
        if (mc.player.input.playerInput.right()) strafe--;

        if (forward == 0 && strafe == 0) {
            if (mc.options.jumpKey.isPressed()) {
                mc.player.setVelocity(vel.x, 1.4, vel.z);
            } else if (mc.options.sneakKey.isPressed()) {
                mc.player.setVelocity(vel.x, -3.6, vel.z);
            } else {
                mc.player.setVelocity(vel.x, 0.0, vel.z);
            }
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            return;
        }

        if (forward != 0) {
            if (strafe > 0) {
                yaw += forward > 0 ? -45 : 45;
            } else if (strafe < 0) {
                yaw += forward > 0 ? 45 : -45;
            }
            strafe = 0;
            forward = forward > 0 ? 1 : -1;
        }

        if (!mc.options.jumpKey.isPressed()) {
            if (mc.options.sneakKey.isPressed()) {
                mc.player.setVelocity(vel.x, -3.6, vel.z);
            } else {
                mc.player.setVelocity(vel.x, 0.0, vel.z);
            }
        } else {
            mc.player.setVelocity(vel.x, forward == 0 && strafe == 0 ? 1.4 : 1.2, vel.z);
        }

        double speed = 0.63;
        float yawNorm = (yaw % 360 + 360) % 360;
        if (isYawInRange(yawNorm, 304, 327) || isYawInRange(yawNorm, 214, 237)
                || isYawInRange(yawNorm, 124, 147) || isYawInRange(yawNorm, 34, 57)) {
            speed = 0.75;
        }
        if (isYawInRange(yawNorm, 306.5f, 324.5f) || isYawInRange(yawNorm, 216.5f, 234.5f)
                || isYawInRange(yawNorm, 126.5f, 144.5f) || isYawInRange(yawNorm, 36.5f, 54.5f)) {
            speed = 0.79;
        }
        if (isYawInRange(yawNorm, 308.7f, 322.7f) || isYawInRange(yawNorm, 218.7f, 232.7f)
                || isYawInRange(yawNorm, 128.7f, 142.7f) || isYawInRange(yawNorm, 38.7f, 52.7f)) {
            speed = 0.81;
        }
        if (isYawInRange(yawNorm, 310.8f, 320.8f) || isYawInRange(yawNorm, 220.8f, 230.8f)
                || isYawInRange(yawNorm, 130.8f, 140.8f) || isYawInRange(yawNorm, 40.8f, 50.8f)) {
            speed = 0.83;
        }
        if (isYawInRange(yawNorm, 311, 319) || isYawInRange(yawNorm, 221, 229)
                || isYawInRange(yawNorm, 131, 139) || isYawInRange(yawNorm, 41, 49)) {
            speed = 0.85;
        }
        if (isYawInRange(yawNorm, 313, 317) || isYawInRange(yawNorm, 223, 227)
                || isYawInRange(yawNorm, 133, 137) || isYawInRange(yawNorm, 43, 47)) {
            speed = 0.88;
        }

        double rad = Math.toRadians(yaw + 90);
        double motionX = forward * speed * Math.cos(rad) + strafe * speed * Math.sin(rad);
        double motionZ = forward * speed * Math.sin(rad) - strafe * speed * Math.cos(rad);

        mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
    }

    private boolean isYawInRange(float yaw, float min, float max) {
        return yaw >= min && yaw <= max;
    }
}

