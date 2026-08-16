package fun.ogi.util.rotation.impl.list;

import fun.ogi.module.impl.list.combat.AttackAura;
import fun.ogi.util.StopWatch;
import fun.ogi.util.combat.BestPoint;
import fun.ogi.util.neuro.rotation.AIRotationManager;
import fun.ogi.util.render.math.MathUtil;
import fun.ogi.util.rotation.GCDFixer;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import fun.ogi.util.rotation.RotationUtil;
import fun.ogi.util.rotation.impl.RotationSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.UnaryOperator;

import static fun.ogi.util.MinecraftUtil.mc;

public class NeuroRotation extends RotationSystem {

    private boolean initialized = false;
    private float lastYaw = 0;
    private float lastPitch = 0;
    private float previousDeltaYaw = 0;
    private float previousDeltaPitch = 0;

    private final StopWatch deviationTimer = new StopWatch();
    private float deviationYaw;
    private float deviationPitch;
    private float targetDeviationYaw;
    private float targetDeviationPitch;
    private long deviationInterval = 200;
    private boolean deviationInitialized;

    private final StopWatch speedTimer = new StopWatch();
    private float currentSpeed = 1.5f;
    private long speedInterval = 800;

    private float movementEasingProgress = 0;
    private float movementEasingTotal = 8;
    private UnaryOperator<Float> movementEasing = NeuroRotation::easeInOutCubic;

    private float[][] currentPattern = PATTERN_HORIZONTAL;
    private int patternIndex;
    private int patternTick;
    private int patternStepTicks = 8;
    private float patternAmplitude = 1f;
    private UnaryOperator<Float> patternEasing = NeuroRotation::easeInOutCubic;
    private float jitterYaw;
    private float jitterPitch;

    public NeuroRotation(AttackAura aura) {
        super(aura);
    }

    public void resetState() {
        initialized = false;
        lastYaw = 0;
        lastPitch = 0;
        previousDeltaYaw = 0;
        previousDeltaPitch = 0;
        deviationInitialized = false;
        deviationYaw = 0;
        deviationPitch = 0;
        targetDeviationYaw = 0;
        targetDeviationPitch = 0;
        deviationTimer.reset();
        speedTimer.reset();
        currentSpeed = MathUtil.random(0.8f, 2.2f);
        speedInterval = (long) MathUtil.random(500, 1600);
        movementEasingProgress = 0;
        movementEasingTotal = 8;
        movementEasing = NeuroRotation::easeInOutCubic;
        currentPattern = PATTERN_HORIZONTAL;
        patternIndex = 0;
        patternTick = 0;
        patternStepTicks = 8;
        patternAmplitude = 1f;
        patternEasing = NeuroRotation::easeInOutCubic;
        jitterYaw = 0;
        jitterPitch = 0;
    }

    @Override
    public void update(LivingEntity target) {
        if (target == null || mc.player == null) return;

        if (!initialized) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
            previousDeltaYaw = 0;
            previousDeltaPitch = 0;
            initialized = true;
        }

        Vec3d targetPoint = BestPoint.getMultipoint(target, aura.distance.getValue());
        Vec2f angle = new RotationUtil().calculate(targetPoint);
        float baseYaw = MathHelper.wrapDegrees(angle.x);
        float basePitch = MathHelper.clamp(angle.y, -90f, 90f);

        Vec2f deviated = startDevation(target, baseYaw, basePitch, 16f, 11f, 400);

        updatePatternJitter();

        float targetYaw = MathHelper.wrapDegrees(deviated.x + jitterYaw);
        float targetPitch = MathHelper.clamp(deviated.y + jitterPitch, -90f, 90f);

        float targetDeltaYaw = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float targetDeltaPitch = targetPitch - lastPitch;

        if (!deviationInitialized && Math.abs(targetDeltaYaw) < 1.5f && Math.abs(targetDeltaPitch) < 1.5f) {
            previousDeltaYaw = 0;
            previousDeltaPitch = 0;
            return;
        }

        float[] output = AIRotationManager.predict(new float[]{
                previousDeltaYaw,
                previousDeltaPitch,
                targetDeltaYaw,
                targetDeltaPitch
        });

        updateSpeed();

        float easeFactor = getMovementEase();
        float playerFactor = getPlayerAimFactor();

        float speed = currentSpeed * 2.2f * MathUtil.random(0.9f, 1.4f) * easeFactor * playerFactor;
        float appliedYaw = output[0] * speed + targetDeltaYaw * 0.5f;

        appliedYaw *= MathUtil.random(0.7f, 1.5f);
        appliedYaw = clampTowardTarget(appliedYaw, targetDeltaYaw, MathUtil.random(45f, 75f));
        if (Math.random() < 0.06f) appliedYaw += MathUtil.random(-3f, 3f);
        appliedYaw += MathUtil.random(-1.5f, 1.5f);

        float pitchEase = Math.max(easeFactor, 0.4f);
        float pitchStep = MathHelper.clamp(Math.abs(targetDeltaPitch) * MathUtil.random(0.4f, 0.55f), 2f, MathUtil.random(13f, 18f)) * MathUtil.random(0.7f, 1.4f) * pitchEase;
        float appliedPitch = MathHelper.clamp(targetDeltaPitch, -pitchStep, pitchStep);
        if (Math.random() < 0.06f) appliedPitch += MathUtil.random(-2f, 2f);
        appliedPitch += MathUtil.random(-1.0f, 1.0f);

        float newYaw = MathHelper.wrapDegrees(lastYaw + appliedYaw);
        float newPitch = MathHelper.clamp(lastPitch + appliedPitch, -90f, 90f);

        float gcd = new GCDFixer().getGCDValue();
        newYaw -= (newYaw - lastYaw) % gcd;
        newPitch -= (newPitch - lastPitch) % gcd;

        RotationComponent.update(new Rotation(newYaw, newPitch), 360, 360, 360, 360, 0, 1, aura.clientLook.getValue());

        previousDeltaYaw = MathHelper.wrapDegrees(newYaw - lastYaw);
        previousDeltaPitch = newPitch - lastPitch;

        lastYaw = newYaw;
        lastPitch = newPitch;
    }

    private void updateSpeed() {
        if (speedTimer.finished(speedInterval)) {
            currentSpeed = MathUtil.random(0.8f, 2.2f);
            speedInterval = (long) MathUtil.random(500, 1600);
            speedTimer.reset();
        }
    }

    private float getMovementEase() {
        movementEasingProgress++;
        if (movementEasingProgress >= movementEasingTotal) {
            movementEasingProgress = 0;
            movementEasingTotal = MathUtil.random(5f, 14f);
            movementEasing = EASINGS.get((int) MathUtil.random(0, EASINGS.size()));
        }
        float progress = movementEasingProgress / movementEasingTotal;
        return movementEasing.apply(progress);
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

        float step = MathUtil.random(0.05f, 0.15f);
        deviationYaw += MathHelper.wrapDegrees(targetDeviationYaw - deviationYaw) * step;
        deviationPitch += (targetDeviationPitch - deviationPitch) * step;

        deviationYaw = MathHelper.clamp(deviationYaw + MathUtil.random(-0.15f, 0.15f), -effectiveMaxYaw, effectiveMaxYaw);
        deviationPitch = MathHelper.clamp(deviationPitch + MathUtil.random(-0.125f, 0.125f), -effectiveMaxPitch, effectiveMaxPitch);

        return new Vec2f(yaw + deviationYaw, MathHelper.clamp(pitch + deviationPitch, -90f, 90f));
    }

    private void pickDeviationTarget(float maxYaw, float maxPitch, float time) {
        float roll = MathUtil.random(0f, 1f);
        if (roll < 0.35f) {
            targetDeviationYaw = signedOffset(maxYaw * 0.15f, maxYaw);
            targetDeviationPitch = 0f;
        } else if (roll < 0.7f) {
            targetDeviationYaw = 0f;
            targetDeviationPitch = signedOffset(maxPitch * 0.15f, maxPitch);
        } else {
            targetDeviationYaw = signedOffset(maxYaw * 0.15f, maxYaw);
            targetDeviationPitch = signedOffset(maxPitch * 0.15f, maxPitch);
        }
        deviationInterval = (long) (time * MathUtil.random(0.3f, 0.5f));
    }

    private float signedOffset(float min, float max) {
        if (max <= min) return min;
        float magnitude = MathUtil.random(min, max);
        return Math.random() < 0.5 ? -magnitude : magnitude;
    }

    private float clampTowardTarget(float value, float targetDelta, float maxStep) {
        float direction = Math.signum(targetDelta);
        float limit = Math.min(Math.abs(targetDelta), maxStep);
        return MathHelper.clamp(value, direction < 0 ? -limit : 0f, direction > 0 ? limit : 0f);
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
            NeuroRotation::easeLinear,
            NeuroRotation::easeInQuad,
            NeuroRotation::easeOutQuad,
            NeuroRotation::easeInOutCubic,
            NeuroRotation::easeOutCubic,
            NeuroRotation::easeOutQuart,
            NeuroRotation::easeInOutSine,
            NeuroRotation::easeInOutCirc,
            NeuroRotation::easeOutBack,
            NeuroRotation::easeInOutExpo
    );
}

