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

import java.util.List;
import java.util.function.UnaryOperator;

import static fun.ogi.util.MinecraftUtil.mc;

public class FuntimeRotation extends RotationSystem {
    private static final int MULTIPOINTS = 250;

    private final StopWatch deviationTimer = new StopWatch();
    private float deviationYaw;
    private float deviationPitch;
    private float targetDeviationYaw;
    private float targetDeviationPitch;
    private long deviationInterval = 400;
    private boolean deviationInitialized;

    private float movementEasingProgress = 0;
    private float movementEasingTotal = 8;
    private UnaryOperator<Float> movementEasing = FuntimeRotation::easeInOutCubic;

    private float[][] currentPattern = PATTERN_HORIZONTAL;
    private int patternIndex;
    private int patternTick;
    private int patternStepTicks = 8;
    private float patternAmplitude = 1f;
    private UnaryOperator<Float> patternEasing = FuntimeRotation::easeInOutCubic;
    private float jitterYaw;
    private float jitterPitch;

    public FuntimeRotation(AttackAura aura) {
        super(aura);
    }
    public void resetState() {
        deviationInitialized = false;
        deviationYaw = 0;
        deviationPitch = 0;
        targetDeviationYaw = 0;
        targetDeviationPitch = 0;
        deviationTimer.reset();
        movementEasingProgress = 0;
        movementEasingTotal = 8;
        movementEasing = FuntimeRotation::easeInOutCubic;
        currentPattern = PATTERN_HORIZONTAL;
        patternIndex = 0;
        patternTick = 0;
        patternStepTicks = 8;
        patternAmplitude = 1f;
        patternEasing = FuntimeRotation::easeInOutCubic;
        jitterYaw = 0;
        jitterPitch = 0;
    }

    @Override
    public void update(LivingEntity target) {
        if (target == null) return;

        Vec3d aimPoint = getMultipointAim(target);

        Vec2f angle = new RotationUtil().calculate(aimPoint);
        float baseYaw = MathHelper.wrapDegrees(angle.x);
        float basePitch = MathHelper.clamp(angle.y, -90f, 90f);
        updatePatternJitter();

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
        RotationComponent.update(rotka, 560, 560, 180, 180, 0, 1, aura.clientLook.getValue());
    }

    private Vec2f startDevation(LivingEntity target, float yaw, float pitch, float maxYaw, float maxPitch, float time) {
        if (mc.player == null || mc.world == null) return new Vec2f(yaw, pitch);

        Vec3d targetPos = target.getPos();
        double distance = Math.max(1.5, mc.player.getEyePos().distanceTo(targetPos));
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

        float step = MathUtil.random(1.5f, 1.75f);
        deviationYaw += MathHelper.wrapDegrees(targetDeviationYaw - deviationYaw) * step;
        deviationPitch += (targetDeviationPitch - deviationPitch) * step;

        deviationYaw = MathHelper.clamp(deviationYaw + MathUtil.random(-1.6f, 1.8f), -effectiveMaxYaw, effectiveMaxYaw);
        deviationPitch = MathHelper.clamp(deviationPitch + MathUtil.random(-1.6f, 1.6f), -effectiveMaxPitch, effectiveMaxPitch);

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
    private float getPlayerAimFactor() {
        float speedFactor = (float) Math.min(1.5, 1.0 + mc.player.getMovementSpeed() * 0.5);
        float fallFactor = 1.0f + Math.min(0.4f, Math.abs(mc.player.fallDistance) * 0.03f);
        float swingFactor = 1.0f + mc.player.getAttackCooldownProgress(0) * 0.3f;
        return speedFactor * fallFactor * swingFactor;
    }

    private void updatePatternJitter() {
        patternTick++;
        if (patternTick >= patternStepTicks) {
            patternTick = 0;
            patternIndex = (patternIndex + 1) % currentPattern.length;
            if (Math.random() < 0.3f) {
                currentPattern = PATTERNS[(int) MathUtil.random(0, PATTERNS.length)];
                patternIndex = (int) MathUtil.random(0, currentPattern.length);
            }
            patternEasing = EASINGS.get((int) MathUtil.random(0, EASINGS.size()));
            patternStepTicks = (int) MathUtil.random(4, 10);
            patternAmplitude = MathUtil.random(0.4f, 2.2f);
        }
        float progress = patternTick / (float) patternStepTicks;
        float eased = patternEasing.apply(progress);
        float[] from = currentPattern[(patternIndex - 1 + currentPattern.length) % currentPattern.length];
        float[] to = currentPattern[patternIndex];
        float amplitude = patternAmplitude * getPlayerAimFactor();
        jitterYaw = (from[0] + (to[0] - from[0]) * eased) * amplitude;
        jitterPitch = (from[1] + (to[1] - from[1]) * eased) * amplitude;
    }

    private static float easeLinear(float x) {
        return x;
    }

    private static float easeInQuad(float x) {
        return x * x;
    }

    private static float easeOutQuad(float x) {
        return 1f - (1f - x) * (1f - x);
    }

    private static float easeInOutCubic(float x) {
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }

    private static float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1f - x, 3);
    }

    private static float easeOutQuart(float x) {
        return 1f - (float) Math.pow(1f - x, 4);
    }

    private static float easeInOutSine(float x) {
        return (float) (-(Math.cos(Math.PI * x) - 1) / 2);
    }

    private static float easeInOutCirc(float x) {
        return x < 0.5f
                ? (float) (0.5f * (1 - Math.sqrt(1 - 4 * x * x)))
                : (float) (0.5f * (Math.sqrt(-(2 * x - 3) * (2 * x - 1)) + 1));
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        return 1f + c3 * (float) Math.pow(x - 1f, 3) + c1 * (float) Math.pow(x - 1f, 2);
    }

    private static float easeInOutExpo(float x) {
        return x == 0f ? 0f : x == 1f ? 1f
                : x < 0.5f ? (float) Math.pow(2, 20 * x - 10) / 2f
                : (2f - (float) Math.pow(2, -20 * x + 10)) / 2f;
    }

    private static final float[][] PATTERN_HORIZONTAL = {
            {-1.6f, 0.2f}, {1.3f, -0.1f}, {-0.9f, 0.3f}, {1.5f, 0.1f}, {-1.2f, -0.2f}, {0.8f, 0.4f}
    };
    private static final float[][] PATTERN_VERTICAL = {
            {0.2f, -1.4f}, {-0.1f, 1.2f}, {0.3f, -0.8f}, {-0.2f, 1.5f}, {0.1f, -1.0f}, {-0.3f, 0.7f}
    };
    private static final float[][] PATTERN_DIAGONAL = {
            {-1.3f, 1.0f}, {1.2f, -0.9f}, {-1.0f, -1.1f}, {1.4f, 1.2f}, {-1.1f, 0.8f}, {0.9f, -1.3f}
    };
    private static final float[][] PATTERN_CIRCLE = {
            {0f, 1.4f}, {0.8f, 1.2f}, {1.3f, 0.6f}, {1.4f, 0f}, {1.3f, -0.6f}, {0.8f, -1.2f},
            {0f, -1.4f}, {-0.8f, -1.2f}, {-1.3f, -0.6f}, {-1.4f, 0f}, {-1.3f, 0.6f}, {-0.8f, 1.2f}
    };
    private static final float[][] PATTERN_STAR = {
            {-1.4f, -1.1f}, {1.5f, 1.0f}, {-1.1f, 1.3f}, {1.2f, -1.2f}, {1.0f, 0.2f}, {-0.7f, -0.6f}
    };
    private static final float[][][] PATTERNS = {
            PATTERN_HORIZONTAL, PATTERN_VERTICAL, PATTERN_DIAGONAL, PATTERN_CIRCLE, PATTERN_STAR
    };

    private static final List<UnaryOperator<Float>> EASINGS = List.of(
            FuntimeRotation::easeLinear,
            FuntimeRotation::easeInQuad,
            FuntimeRotation::easeOutQuad,
            FuntimeRotation::easeInOutCubic,
            FuntimeRotation::easeOutCubic,
            FuntimeRotation::easeOutQuart,
            FuntimeRotation::easeInOutSine,
            FuntimeRotation::easeInOutCirc,
            FuntimeRotation::easeOutBack,
            FuntimeRotation::easeInOutExpo
    );
}

