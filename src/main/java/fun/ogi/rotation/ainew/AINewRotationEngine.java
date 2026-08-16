package fun.ogi.rotation.ainew;

import fun.ogi.util.rotation.Rotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public final class AINewRotationEngine {
    private static final long PRE_ATTACK_WINDOW_MS = 5000L;
    private static final float ON_TARGET_DEG = 0.9f;
    private static final int RECENT_CLIP_MEMORY = 3;
    private static final long CLIP_REPLAY_COOLDOWN_MS = 1600L;
    private static final long CLIP_REPLAY_COOLDOWN_JITTER_MS = 1000L;
    private static final long FOLLOWUP_BLOCK_MS = 8000L;
    private final ArrayDeque<AINewRotationProfile.MouseTrial> recentClips = new ArrayDeque<AINewRotationProfile.MouseTrial>();
    private final IdentityHashMap<AINewRotationProfile.MouseTrial, Long> clipReplayCooldownUntil = new IdentityHashMap<AINewRotationProfile.MouseTrial, Long>();
    private final ArrayList<Transition> recentTransitions = new ArrayList<Transition>();
    private AINewRotationProfile.MouseTrial lastClip;
    private AINewRotationProfile profile;
    private final AINewMotionPlayback playback = new AINewMotionPlayback();
    private LivingEntity aimTarget;
    private Vec3d trackedAim;
    private Vec3d lastCenter;
    private float perceivedPitch;
    private boolean hasPerceivedPitch;
    private float gcdResidualYaw;
    private float gcdResidualPitch;
    private long clipCycleAttackMs = -1L;
    private boolean overshootApplied;
    private float overshootAmount;
    private float swayAmpYaw = 0.1f;
    private float swayAmpPitch = 0.06f;
    private float swayFreqYaw = 1.6f;
    private float swayFreqPitch = 1.2f;
    private float swayPhaseYaw;
    private float swayPhasePitch;
    private boolean swayActive;
    private long swayStartNano;
    private float prevSwayYaw;
    private float prevSwayPitch;
    private float driftAmpYaw = 0.16f;
    private float driftAmpPitch = 0.1f;
    private float driftFreqYaw = 0.9f;
    private float driftFreqPitch = 0.7f;
    private float driftPhaseYaw;
    private float driftPhasePitch;

    public void setProfile(AINewRotationProfile profile) {
        if (profile == null) {
            this.profile = null;
            this.reset();
            return;
        }
        if (this.isSameProfile(profile)) {
            return;
        }
        this.profile = profile;
        this.reset();
    }

    public void reset() {
        this.aimTarget = null;
        this.trackedAim = null;
        this.lastCenter = null;
        this.hasPerceivedPitch = false;
        this.gcdResidualYaw = 0.0f;
        this.gcdResidualPitch = 0.0f;
        this.clipCycleAttackMs = -1L;
        this.overshootApplied = false;
        this.overshootAmount = 0.0f;
        this.swayActive = false;
        this.swayStartNano = 0L;
        this.prevSwayYaw = 0.0f;
        this.prevSwayPitch = 0.0f;
        this.rollHumanizers();
        this.recentClips.clear();
        this.clipReplayCooldownUntil.clear();
        this.recentTransitions.clear();
        this.lastClip = null;
        this.playback.reset();
    }

    private void rollHumanizers() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        this.swayAmpYaw = 0.07f + r.nextFloat() * 0.08f;
        this.swayAmpPitch = 0.04f + r.nextFloat() * 0.05f;
        this.swayFreqYaw = 1.1f + r.nextFloat() * 1.4f;
        this.swayFreqPitch = 0.8f + r.nextFloat() * 1.2f;
        this.swayPhaseYaw = r.nextFloat() * 6.2831855f;
        this.swayPhasePitch = r.nextFloat() * 6.2831855f;
        this.driftAmpYaw = 0.1f + r.nextFloat() * 0.16f;
        this.driftAmpPitch = 0.06f + r.nextFloat() * 0.1f;
        this.driftFreqYaw = 0.5f + r.nextFloat() * 1.1f;
        this.driftFreqPitch = 0.4f + r.nextFloat() * 0.9f;
        this.driftPhaseYaw = r.nextFloat() * 6.2831855f;
        this.driftPhasePitch = r.nextFloat() * 6.2831855f;
    }

    public Rotation compute(MinecraftClient client, LivingEntity target, Rotation base, Vec3d eye, Vec3d aimPoint, float frameSeconds, long msUntilAttack, boolean inAttackReach) {
        AINewRotationProfile.MouseTrial clip;
        long effectiveMsUntilAttack;
        float pitchError;
        if (client == null || client.player == null || target == null || this.profile == null || base == null || eye == null) {
            return base;
        }
        if (!this.profile.hasMotionClips()) {
            return base;
        }
        long nowNano = System.nanoTime();
        long nowMs = System.currentTimeMillis();
        Vec3d resolvedAim = this.updateAimPoint(target, aimPoint, frameSeconds);
        Rotation desired = AINewRotationEngine.rotationTo(eye, resolvedAim, base);
        Rotation perceived = this.updatePerceivedDesired(desired, base, frameSeconds);
        perceived = this.applyAimDrift(perceived, base, nowNano);
        float yawError = MathHelper.wrapDegrees((float)(perceived.getYaw() - base.getYaw()));
        float distance = AINewRotationEngine.angularDistance(yawError, pitchError = perceived.getPitch() - base.getPitch());
        if (distance <= ON_TARGET_DEG) {
            this.playback.reset();
            this.clipCycleAttackMs = -1L;
            return this.applyOnTargetSway(client, base, nowNano);
        }
        this.swayActive = false;
        if (this.isNewAttackCycle(effectiveMsUntilAttack = this.normalizeAttackDelay(msUntilAttack, inAttackReach))) {
            this.clipCycleAttackMs = -1L;
        }
        if (this.shouldStartClip(distance, yawError, effectiveMsUntilAttack, inAttackReach) && (clip = AINewMotionPlayback.pickClip(this.profile.getMotionClips(), distance, yawError, this.recentClips, this.clipReplayCooldownUntil, this.lastClip, this.collectBlockedFollowUps(this.lastClip, nowMs), nowMs)) != null) {
            boolean repeated = clip == this.lastClip || this.recentClips.contains(clip);
            this.playback.start(clip, nowNano, distance, yawError, pitchError, repeated);
            this.onClipChosen(clip, nowMs);
            this.clipCycleAttackMs = effectiveMsUntilAttack;
            this.overshootApplied = false;
            this.overshootAmount = 0.1f + ThreadLocalRandom.current().nextFloat() * 0.14f;
        }
        if (!this.playback.isActive()) {
            return base;
        }
        if (distance <= ON_TARGET_DEG && this.playback.getApproachProgress() >= 0.72f) {
            this.finishClip(effectiveMsUntilAttack);
            return base;
        }
        AINewMotionPlayback.Step step = this.playback.poll(nowNano, yawError, pitchError, distance);
        if (step.isFinished()) {
            this.finishClip(effectiveMsUntilAttack);
            return base;
        }
        if (step.isWaiting()) {
            return base;
        }
        float stepYaw = step.getYawDelta();
        float stepPitch = this.applyRecordedPitch(step.getPitchDelta());
        if (this.playback.getApproachProgress() >= 0.36f && distance > ON_TARGET_DEG) {
            float blend = MathHelper.clamp((float)((this.playback.getApproachProgress() - 0.36f) / 0.5f), (float)0.0f, (float)1.0f);
            float close = Math.min(distance * 0.42f, 5.5f) * blend;
            float inv = 1.0f / Math.max(0.001f, distance);
            float pull = blend * 0.42f;
            stepYaw = MathHelper.lerp((float)(1.0f - pull), (float)stepYaw, (float)(yawError * inv * close));
            stepPitch = MathHelper.lerp((float)(1.0f - pull), (float)stepPitch, (float)(pitchError * inv * close));
        }
        if (!this.overshootApplied && distance > 3.0f) {
            float stepMag = (float)Math.sqrt(stepYaw * stepYaw + stepPitch * stepPitch);
            if (stepMag > 0.5f && stepMag >= distance * 0.8f && this.playback.getApproachProgress() < 0.92f) {
                float overshootDeg = Math.min(distance * this.overshootAmount, 2.6f);
                float k = (stepMag + overshootDeg) / stepMag;
                stepYaw *= k;
                stepPitch *= k;
                this.overshootApplied = true;
            }
        }
        float nextYaw = base.getYaw() + stepYaw;
        float nextPitch = MathHelper.clamp((float)(base.getPitch() + stepPitch), (float)-85.0f, (float)85.0f);
        
        nextYaw += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.06f;
        nextPitch += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.04f;
        return this.applyGcd(client, base, nextYaw, nextPitch, true);
    }

    private void finishClip(long effectiveMsUntilAttack) {
        this.playback.reset();
        this.clipCycleAttackMs = effectiveMsUntilAttack;
    }

    private Rotation applyAimDrift(Rotation perceived, Rotation base, long nowNano) {
        float t = (float)((double)Math.floorMod(nowNano, 1000000000000L) / 1.0E9);
        float dy = this.driftAmpYaw * MathHelper.sin((float)(t * this.driftFreqYaw + this.driftPhaseYaw));
        float dp = this.driftAmpPitch * MathHelper.sin((float)(t * this.driftFreqPitch + this.driftPhasePitch));
        return AINewRotationHelper.normalize(new Rotation(perceived.getYaw() + dy, MathHelper.clamp((float)(perceived.getPitch() + dp), (float)-90.0f, (float)90.0f)), base);
    }

    private Rotation applyOnTargetSway(MinecraftClient client, Rotation base, long nowNano) {
        if (!this.swayActive) {
            this.swayActive = true;
            this.swayStartNano = nowNano;
        }
        float t = (float)((double)(nowNano - this.swayStartNano) / 1.0E9);
        float curYaw = this.swayAmpYaw * (MathHelper.sin((float)(t * this.swayFreqYaw + this.swayPhaseYaw)) + 0.4f * MathHelper.sin((float)(t * this.swayFreqYaw * 2.7f + this.swayPhasePitch)));
        float curPitch = this.swayAmpPitch * MathHelper.sin((float)(t * this.swayFreqPitch + this.swayPhasePitch));
        if (this.swayStartNano == nowNano) {
            this.prevSwayYaw = curYaw;
            this.prevSwayPitch = curPitch;
        }
        float dYaw = curYaw - this.prevSwayYaw;
        float dPitch = curPitch - this.prevSwayPitch;
        this.prevSwayYaw = curYaw;
        this.prevSwayPitch = curPitch;
        if (Math.abs(dYaw) < 1.0E-4f && Math.abs(dPitch) < 1.0E-4f) {
            return base;
        }
        return this.applyGcd(client, base, base.getYaw() + dYaw, MathHelper.clamp((float)(base.getPitch() + dPitch), (float)-90.0f, (float)90.0f), false);
    }

    private boolean isNewAttackCycle(long msUntilAttack) {
        if (this.clipCycleAttackMs < 0L) {
            return false;
        }
        return msUntilAttack > this.clipCycleAttackMs + 450L;
    }

    private long normalizeAttackDelay(long msUntilAttack, boolean inAttackReach) {
        if (!inAttackReach) {
            return Math.min(msUntilAttack, PRE_ATTACK_WINDOW_MS);
        }
        if (msUntilAttack <= 0L) {
            return 1800L;
        }
        return msUntilAttack;
    }

    private float applyRecordedPitch(float recordedPitch) {
        if (Math.abs(recordedPitch) <= 1.0E-4f) {
            return 0.0f;
        }
        if (this.playback.isLookAwayPhase() && this.playback.getApproachProgress() < 0.32f && this.profile.isYawDominantStyle()) {
            return recordedPitch * 0.35f;
        }
        return recordedPitch;
    }

    private boolean shouldStartClip(float distance, float yawError, long msUntilAttack, boolean inAttackReach) {
        if (this.playback.isActive() && !this.playback.isFinished()) {
            return false;
        }
        if (distance <= ON_TARGET_DEG) {
            return false;
        }
        if (inAttackReach) {
            if (msUntilAttack > PRE_ATTACK_WINDOW_MS) {
                return false;
            }
            return distance >= 1.0f || Math.abs(yawError) >= 3.5f;
        }
        return distance >= 1.5f || Math.abs(yawError) >= 5.5f;
    }

    private Vec3d updateAimPoint(LivingEntity target, Vec3d aimPoint, float frameSeconds) {
        Vec3d desired = aimPoint != null ? aimPoint : AINewRotationEngine.pickFallbackAimPoint(target);
        Vec3d center = target.getBoundingBox().getCenter();
        if (target != this.aimTarget) {
            this.aimTarget = target;
            this.trackedAim = desired;
            this.lastCenter = center;
            this.hasPerceivedPitch = false;
            this.playback.reset();
            this.clipCycleAttackMs = -1L;
            return this.trackedAim;
        }
        float dt = Math.max(0.001f, frameSeconds);
        float reactionMs = this.profile != null ? Math.max(140.0f, this.profile.getAverageReactionMs()) : 220.0f;
        float pitchTau = Math.max(0.12f, reactionMs * 9.5E-4f);
        float pitchAlpha = 1.0f - (float)Math.exp(-dt / pitchTau);
        if (this.trackedAim == null) {
            this.trackedAim = desired;
        } else {
            double y = this.trackedAim.y + (desired.y - this.trackedAim.y) * (double)pitchAlpha;
            this.trackedAim = new Vec3d(desired.x, y, desired.z);
        }
        this.lastCenter = center;
        return this.trackedAim;
    }

    private Rotation updatePerceivedDesired(Rotation desired, Rotation base, float frameSeconds) {
        float dt = Math.max(0.001f, frameSeconds);
        float reactionMs = this.profile != null ? Math.max(140.0f, this.profile.getAverageReactionMs()) : 220.0f;
        float pitchTau = Math.max(0.14f, reactionMs * 0.00105f);
        float pitchAlpha = 1.0f - (float)Math.exp(-dt / pitchTau);
        if (!this.hasPerceivedPitch) {
            this.perceivedPitch = desired.getPitch();
            this.hasPerceivedPitch = true;
        } else {
            this.perceivedPitch = MathHelper.lerp((float)pitchAlpha, (float)this.perceivedPitch, (float)desired.getPitch());
        }
        return AINewRotationHelper.normalize(new Rotation(desired.getYaw(), this.perceivedPitch), base);
    }

    private static Vec3d pickFallbackAimPoint(LivingEntity target) {
        Box box = target.getBoundingBox();
        double height = Math.max(0.01, box.getLengthY());
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        double y = box.minY + height * 0.42;
        return new Vec3d(centerX, y, centerZ);
    }

    private Rotation applyGcd(MinecraftClient client, Rotation base, float rawYaw, float rawPitch, boolean clipPlayback) {
        float yawDelta = MathHelper.wrapDegrees((float)(rawYaw - base.getYaw()));
        float pitchDelta = rawPitch - base.getPitch();
        float gcd = AINewRotationEngine.getMouseGcd(client);
        float steppedYaw = this.consumeGcdDelta(yawDelta, gcd, true, clipPlayback);
        float steppedPitch = this.consumeGcdDelta(pitchDelta, gcd, false, clipPlayback);
        return AINewRotationHelper.normalize(new Rotation(base.getYaw() + steppedYaw, MathHelper.clamp((float)(base.getPitch() + steppedPitch), (float)-90.0f, (float)90.0f)), base);
    }

    private float consumeGcdDelta(float delta, float gcd, boolean yaw, boolean clipPlayback) {
        float residual = yaw ? this.gcdResidualYaw : this.gcdResidualPitch;
        if (gcd <= 1.0E-5f) {
            if (yaw) {
                this.gcdResidualYaw = 0.0f;
            } else {
                this.gcdResidualPitch = 0.0f;
            }
            return delta;
        }
        float accum = delta + residual;
        float magnitude = Math.abs(accum);
        float stepped = 0.0f;
        float threshold = clipPlayback ? 0.12f : 0.35f;
        if (magnitude >= gcd * threshold) {
            stepped = Math.copySign((float)Math.floor(magnitude / gcd) * gcd, accum);
        } else if (clipPlayback && magnitude >= gcd * 0.04f) {
            stepped = Math.copySign(gcd, accum);
        }
        float nextResidual = MathHelper.clamp((float)(accum - stepped), (float)(-gcd * 2.0f), (float)(gcd * 2.0f));
        if (yaw) {
            this.gcdResidualYaw = nextResidual;
        } else {
            this.gcdResidualPitch = nextResidual;
        }
        return stepped;
    }

    private boolean isSameProfile(AINewRotationProfile next) {
        if (this.profile == null || next == null) {
            return false;
        }
        return this.profile.getName().equalsIgnoreCase(next.getName()) && this.profile.getCreatedAtMs() == next.getCreatedAtMs() && this.profile.getHitCount() == next.getHitCount() && this.profile.getMotionClips().size() == next.getMotionClips().size();
    }

    private Set<AINewRotationProfile.MouseTrial> collectBlockedFollowUps(AINewRotationProfile.MouseTrial from, long nowMs) {
        if (from == null || this.recentTransitions.isEmpty()) {
            return Collections.emptySet();
        }
        Set<AINewRotationProfile.MouseTrial> blocked = Collections.newSetFromMap(new IdentityHashMap<AINewRotationProfile.MouseTrial, Boolean>());
        Iterator<Transition> it = this.recentTransitions.iterator();
        while (it.hasNext()) {
            Transition t = it.next();
            if (t.untilMs <= nowMs) {
                it.remove();
                continue;
            }
            if (t.from == from) {
                blocked.add(t.to);
            }
        }
        return blocked;
    }

    private void onClipChosen(AINewRotationProfile.MouseTrial clip, long nowMs) {
        if (clip == null) {
            return;
        }
        if (this.lastClip != null && this.lastClip != clip) {
            this.recentTransitions.add(new Transition(this.lastClip, clip, nowMs + FOLLOWUP_BLOCK_MS));
        }
        this.recentClips.addLast(clip);
        while (this.recentClips.size() > RECENT_CLIP_MEMORY) {
            this.recentClips.removeFirst();
        }
        long cooldown = CLIP_REPLAY_COOLDOWN_MS + ThreadLocalRandom.current().nextLong(0L, CLIP_REPLAY_COOLDOWN_JITTER_MS + 1L);
        this.clipReplayCooldownUntil.put(clip, nowMs + cooldown);
        this.lastClip = clip;
        this.pruneCooldowns(nowMs);
    }

    private void pruneCooldowns(long nowMs) {
        if (this.clipReplayCooldownUntil.size() <= 32) {
            return;
        }
        Iterator<Map.Entry<AINewRotationProfile.MouseTrial, Long>> it = this.clipReplayCooldownUntil.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() > nowMs) continue;
            it.remove();
        }
    }

    private static float angularDistance(float yawDelta, float pitchDelta) {
        return (float)Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    private static Rotation rotationTo(Vec3d from, Vec3d to, Rotation base) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.max(0.001, horiz))));
        pitch = MathHelper.clamp((float)pitch, (float)-85.0f, (float)85.0f);
        return AINewRotationHelper.normalize(new Rotation(yaw, pitch), base);
    }

    private static float getMouseGcd(MinecraftClient client) {
        double sensitivity = (Double)client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return (float)(sensitivity * sensitivity * sensitivity * 8.0 * 0.15);
    }

    private static final class Transition {
        private final AINewRotationProfile.MouseTrial from;
        private final AINewRotationProfile.MouseTrial to;
        private final long untilMs;

        private Transition(AINewRotationProfile.MouseTrial from, AINewRotationProfile.MouseTrial to, long untilMs) {
            this.from = from;
            this.to = to;
            this.untilMs = untilMs;
        }
    }
}

