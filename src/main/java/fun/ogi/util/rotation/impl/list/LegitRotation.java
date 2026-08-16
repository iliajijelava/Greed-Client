package fun.ogi.util.rotation.impl.list;

import fun.ogi.module.impl.list.combat.AttackAura;
import fun.ogi.util.StopWatch;
import fun.ogi.util.combat.RayTraceUtil;
import fun.ogi.util.render.math.MathUtil;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import fun.ogi.util.rotation.RotationUtil;
import fun.ogi.util.rotation.impl.RotationSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import static fun.ogi.util.MinecraftUtil.mc;

public class LegitRotation extends RotationSystem {

    private static final int MULTIPOINTS = 250;

    private final StopWatch deviationTimer = new StopWatch();
    private float deviationYaw;
    private float deviationPitch;
    private float targetDeviationYaw;
    private float targetDeviationPitch;
    private long deviationInterval = 400;
    private boolean deviationInitialized;

    public LegitRotation(AttackAura aura) {
        super(aura);
    }

    @Override
    public void update(LivingEntity target) {
        if (target == null) return;

        Vec3d aimPoint = getMultipointAim(target);

        Vec2f angle = new RotationUtil().calculate(aimPoint);
        float baseYaw = MathHelper.wrapDegrees(angle.x);
        float basePitch = MathHelper.clamp(angle.y, -90f, 90f);

        Vec2f deviated = startDevation(target, baseYaw, basePitch, 14f, 10f, 250);

        float targetYaw = MathHelper.wrapDegrees(deviated.x);
        float targetPitch = MathHelper.clamp(deviated.y, -90f, 90f);

        rotateToward(targetYaw, targetPitch);
    }

    private void rotateToward(float targetYaw, float targetPitch) {
        float maxStep = Math.max(aura.rotationSpeed.getFloatValue(), 30f);

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - mc.player.getYaw());
        float deltaPitch = targetPitch - mc.player.getPitch();

        float newYaw = mc.player.getYaw() + MathHelper.clamp(deltaYaw, -maxStep, maxStep);
        float newPitch = MathHelper.clamp(mc.player.getPitch() + MathHelper.clamp(deltaPitch, -maxStep, maxStep), -90f, 90f);
        Rotation rotka = new Rotation(newYaw, newPitch);
        RotationComponent.update(rotka, 360, 360, 180, 180, 0, 1, aura.clientLook.getValue());
    }

    private Vec2f startDevation(LivingEntity target, float yaw, float pitch, float maxYaw, float maxPitch, float time) {
        if (mc.player == null || mc.world == null) return new Vec2f(yaw, pitch);

        Vec3d targetPos = target.getPos();
        double distance = Math.max(0.5, mc.player.getEyePos().distanceTo(targetPos));
        float scale = (float) MathUtil.clamp(3.0 / distance, 0.5, 3.0);
        float effectiveMaxYaw = maxYaw * scale;
        float effectiveMaxPitch = maxPitch * scale;

        if (!deviationInitialized) {
            pickDeviationTarget(effectiveMaxYaw, effectiveMaxPitch, time);
            deviationYaw = targetDeviationYaw;
            deviationPitch = targetDeviationPitch;
            deviationTimer.reset();
            deviationInitialized = true;
        }

        if (deviationTimer.finished(deviationInterval)) {
            pickDeviationTarget(effectiveMaxYaw, effectiveMaxPitch, time);
            deviationTimer.reset();
        }

        float step = MathUtil.random(0.5f, 0.75f);
        deviationYaw += MathHelper.wrapDegrees(targetDeviationYaw - deviationYaw) * step;
        deviationPitch += (targetDeviationPitch - deviationPitch) * step;

        deviationYaw = MathHelper.clamp(deviationYaw + MathUtil.random(-0.8f, 0.8f), -effectiveMaxYaw, effectiveMaxYaw);
        deviationPitch = MathHelper.clamp(deviationPitch + MathUtil.random(-0.6f, 0.6f), -effectiveMaxPitch, effectiveMaxPitch);

        return new Vec2f(yaw + deviationYaw, MathHelper.clamp(pitch + deviationPitch, -90f, 90f));
    }

    private void pickDeviationTarget(float maxYaw, float maxPitch, float time) {
        float roll = MathUtil.random(0f, 1f);
        if (roll < 0.35f) {
            targetDeviationYaw = signedOffset(maxYaw * 0.4f, maxYaw);
            targetDeviationPitch = 0f;
        } else if (roll < 0.7f) {
            targetDeviationYaw = 0f;
            targetDeviationPitch = signedOffset(maxPitch * 0.4f, maxPitch);
        } else {
            targetDeviationYaw = signedOffset(maxYaw * 0.4f, maxYaw);
            targetDeviationPitch = signedOffset(maxPitch * 0.4f, maxPitch);
        }
        deviationInterval = (long) (time * MathUtil.random(0.6f, 1.5f));
    }

    private float signedOffset(float min, float max) {
        if (max <= min) return min;
        float magnitude = MathUtil.random(min, max);
        return Math.random() < 0.5 ? -magnitude : magnitude;
    }

    private Vec3d getMultipointAim(LivingEntity target) {
        Box box = target.getBoundingBox();
        int n = (int) Math.max(2, Math.ceil(Math.cbrt(MULTIPOINTS)));
        double stepX = (box.maxX - box.minX) / (n - 1);
        double stepY = (box.maxY - box.minY) / (n - 1);
        double stepZ = (box.maxZ - box.minZ) / (n - 1);

        Vec3d eye = mc.player.getEyePos();
        Vec3d best = null;
        double bestDistance = Double.MAX_VALUE;
        int sampled = 0;

        for (int ix = 0; ix < n; ix++) {
            for (int iy = 0; iy < n; iy++) {
                for (int iz = 0; iz < n; iz++) {
                    if (sampled++ >= MULTIPOINTS) break;

                    Vec3d point = new Vec3d(box.minX + stepX * ix, box.minY + stepY * iy, box.minZ + stepZ * iz);

                    if (!aura.throughWalls.getValue() && !isPointVisible(eye, point, box)) {
                        continue;
                    }

                    double distance = eye.squaredDistanceTo(point);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = point;
                    }
                }
            }
        }

        return best != null ? best : target.getEyePos();
    }

    private boolean isPointVisible(Vec3d eye, Vec3d point, Box box) {
        float distance = (float) eye.distanceTo(point);
        if (distance <= 0.1f) return true;

        Vec3d direction = point.subtract(eye).normalize();
        if (!RayTraceUtil.rayTrace(direction, distance, box)) return false;

        return mc.world.raycast(new RaycastContext(eye, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }
}

