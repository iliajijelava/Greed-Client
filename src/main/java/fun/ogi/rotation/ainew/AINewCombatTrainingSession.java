package fun.ogi.rotation.ainew;

import fun.ogi.util.rotation.Rotation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.EntityHitResult;

public final class AINewCombatTrainingSession {
    private static final long MIN_SAMPLE_MS = 4L;
    private static final long MAX_SAMPLE_MS = 140L;
    private final String profileName;
    private final AINewRotationManager manager;
    private final long trainingDurationMs;
    private final long startedAtMs;
    private final List<TimeSample> timeline = new ArrayList<TimeSample>();
    private final List<Long> attackTimesMs = new ArrayList<Long>();
    private boolean finished;
    private int actionBarCooldown;
    private boolean hasLastRotation;
    private float lastYaw;
    private float lastPitch;
    private long lastSampleNano;
    private double totalMoveDeg;
    private double totalYawMoveDeg;
    private double totalPitchMoveDeg;
    private int hitCount;
    private int sampleCount;
    private LivingEntity focusTarget;

    public AINewCombatTrainingSession(String profileName, AINewRotationManager manager, long trainingDurationMs) {
        this.profileName = profileName;
        this.manager = manager;
        this.trainingDurationMs = Math.max(15000L, trainingDurationMs);
        this.startedAtMs = System.currentTimeMillis();
        this.lastSampleNano = System.nanoTime();
    }

    public String getProfileName() {
        return this.profileName;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public long getRemainingMs() {
        return Math.max(0L, this.trainingDurationMs - (System.currentTimeMillis() - this.startedAtMs));
    }

    public void tick(MinecraftClient client) {
        if (this.finished || client == null || client.player == null) {
            return;
        }
        long nowMs = System.currentTimeMillis();
        if (nowMs - this.startedAtMs >= this.trainingDurationMs) {
            this.finish();
            return;
        }
        if (--this.actionBarCooldown <= 0) {
            this.actionBarCooldown = 12;
            long remainingSec = (this.getRemainingMs() + 999L) / 1000L;
            client.player.sendMessage(Text.literal("[AINew] Training: " + AINewCombatTrainingSession.formatTime(remainingSec) + " | samples " + this.sampleCount + " | hits " + this.hitCount + " | clips~ " + Math.max(0, this.attackTimesMs.size())), true);
        }
    }

    public void recordRealtime(MinecraftClient client) {
        if (this.finished || client == null || client.player == null) {
            return;
        }
        long nowNano = System.nanoTime();
        long nowMs = System.currentTimeMillis();
        if (nowMs - this.startedAtMs >= this.trainingDurationMs) {
            this.finish();
            return;
        }
        float yaw = client.player.getYaw();
        float pitch = client.player.getPitch();
        TargetContext ctx = this.resolveTargetContext(client, yaw, pitch);
        int deltaMs = 16;
        if (this.lastSampleNano > 0L) {
            long rawDeltaMs = (nowNano - this.lastSampleNano) / 1000000L;
            deltaMs = (int)MathHelper.clamp((long)rawDeltaMs, (long)4L, (long)140L);
        }
        this.lastSampleNano = nowNano;
        if (!this.hasLastRotation) {
            this.lastYaw = yaw;
            this.lastPitch = pitch;
            this.hasLastRotation = true;
            this.timeline.add(new TimeSample(deltaMs, 0.0f, 0.0f, ctx.angularErrorDeg(), 0.0f, 0.0f, ctx.worldDistanceNorm(), ctx.pitchErrorNorm(), 0.0f, nowMs - this.startedAtMs, ctx.hasAimAxes()));
            ++this.sampleCount;
            return;
        }
        float yawDelta = MathHelper.wrapDegrees((float)(yaw - this.lastYaw));
        float pitchDelta = pitch - this.lastPitch;
        if (Math.abs(yawDelta) > 0.8f && Math.abs(pitchDelta) < 0.25f) {
            pitchDelta = 0.0f;
        }
        float forwardMoveDeg = 0.0f;
        float lateralMoveDeg = 0.0f;
        float moveDeg = AINewCombatTrainingSession.angularDistance(yawDelta, pitchDelta);
        if (moveDeg > 1.0E-4f && ctx.hasAimAxes()) {
            forwardMoveDeg = yawDelta * ctx.dirYaw() + pitchDelta * ctx.dirPitch();
            lateralMoveDeg = yawDelta * ctx.perpYaw() + pitchDelta * ctx.perpPitch();
        }
        this.totalMoveDeg += (double)moveDeg;
        this.totalYawMoveDeg += (double)Math.abs(yawDelta);
        this.totalPitchMoveDeg += (double)Math.abs(pitchDelta);
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        float preAttackNorm = this.computePreAttackNorm(nowMs - this.startedAtMs);
        this.timeline.add(new TimeSample(deltaMs, yawDelta, pitchDelta, ctx.angularErrorDeg(), forwardMoveDeg, lateralMoveDeg, ctx.worldDistanceNorm(), ctx.pitchErrorNorm(), preAttackNorm, nowMs - this.startedAtMs, ctx.hasAimAxes()));
        ++this.sampleCount;
    }

    public void onAttack(Entity target) {
        LivingEntity living;
        if (this.finished || target == null || target == MinecraftClient.getInstance().player) {
            return;
        }
        if (target instanceof LivingEntity && (living = (LivingEntity)target).isAlive()) {
            this.focusTarget = living;
        }
        ++this.hitCount;
        this.attackTimesMs.add(System.currentTimeMillis() - this.startedAtMs);
    }

    private float computePreAttackNorm(long absMs) {
        float best = 0.0f;
        for (Long attackMs : this.attackTimesMs) {
            long delta;
            if (attackMs == null || (delta = attackMs - absMs) < 0L || delta > 3200L) continue;
            best = Math.max(best, 1.0f - (float)delta / 3200.0f);
        }
        return best;
    }

    public void stop() {
        this.finish(true);
    }

    private void finish() {
        this.finish(false);
    }

    private void finish(boolean stoppedEarly) {
        if (this.finished) {
            return;
        }
        this.finished = true;
        List<AINewRotationProfile.MouseTrial> clips = AINewMotionClipBuilder.buildClips(this.timeline, this.attackTimesMs);
        long durationMs = Math.max(1L, System.currentTimeMillis() - this.startedAtMs);
        float avgSpeed = (float)(this.totalMoveDeg / ((double)durationMs / 1000.0));
        float profileSpeed = avgSpeed > 0.0f ? avgSpeed : 35.0f;
        float avgReaction = AINewCombatTrainingSession.averageClipReaction(clips);
        AINewRotationProfile profile = new AINewRotationProfile(this.profileName, System.currentTimeMillis(), this.trainingDurationMs, this.hitCount, avgReaction, profileSpeed, clips);
        this.manager.addOrReplace(profile);
        this.sendTrainingReport(profile, clips.size(), stoppedEarly);
    }

    private void sendTrainingReport(AINewRotationProfile profile, int clipCount, boolean stoppedEarly) {
        MinecraftClient mcClient = MinecraftClient.getInstance();
        if (mcClient.player == null) return;
        if (stoppedEarly) {
            mcClient.player.sendMessage(Text.literal("[AINew] Training stopped: " + this.profileName), false);
        } else {
            mcClient.player.sendMessage(Text.literal("[AINew] Training complete: " + this.profileName), false);
        }
        long durationSec = Math.max(1L, (System.currentTimeMillis() - this.startedAtMs) / 1000L);
        mcClient.player.sendMessage(Text.literal("[AINew] " + durationSec + "s | Samples: " + this.sampleCount + " | Hits: " + this.hitCount), false);
        mcClient.player.sendMessage(Text.literal("[AINew] Speed: " + String.format("%.1f", Float.valueOf(profile.getAverageSpeedDegPerSec())) + " deg/s"), false);
        mcClient.player.sendMessage(Text.literal("[AINew] Motion clips: " + clipCount + " | Reaction: " + String.format("%.0f", Float.valueOf(profile.getAverageReactionMs())) + "ms"), false);
        if (this.sampleCount < 80) {
            mcClient.player.sendMessage(Text.literal("[AINew] Few samples - move camera longer and more actively"), false);
        } else if (this.hitCount < 3) {
            mcClient.player.sendMessage(Text.literal("[AINew] Few hits - attack mobs to record pre-attack"), false);
        } else if (clipCount < 4) {
            mcClient.player.sendMessage(Text.literal("[AINew] Few clips - do look-away then pause then flick then attack"), false);
        } else {
            mcClient.player.sendMessage(Text.literal("[AINew] Profile saved. KillAura -> AINewRot (AI) -> " + this.profileName), false);
        }
    }

    private static float averageClipReaction(List<AINewRotationProfile.MouseTrial> clips) {
        if (clips.isEmpty()) {
            return 220.0f;
        }
        double sum = 0.0;
        for (AINewRotationProfile.MouseTrial clip : clips) {
            sum += (double)clip.getDurationMs();
        }
        return (float)(sum / (double)clips.size());
    }

    private TargetContext resolveTargetContext(MinecraftClient client, float yaw, float pitch) {
        LivingEntity target;
        LivingEntity crosshair = AINewCombatTrainingSession.resolveLookTarget(client);
        if (crosshair != null) {
            this.focusTarget = crosshair;
        }
        if ((target = crosshair) == null && this.focusTarget != null && this.focusTarget.isAlive() && !this.focusTarget.isRemoved()) {
            target = this.focusTarget;
        }
        if (target == null && (target = AINewCombatTrainingSession.resolveNearestCombatTarget(client)) != null) {
            this.focusTarget = target;
        }
        if (target == null) {
            return TargetContext.free();
        }
        Vec3d eye = client.player.getEyePos();
        Vec3d aim = AINewCombatTrainingSession.pickAimPoint(target);
        float worldDistance = (float)eye.distanceTo(aim);
        Rotation desired = AINewCombatTrainingSession.rotationTo(eye, aim, yaw, pitch);
        float yawError = MathHelper.wrapDegrees((float)(desired.getYaw() - yaw));
        float pitchError = desired.getPitch() - pitch;
        float angularError = AINewCombatTrainingSession.angularDistance(yawError, pitchError);
        float inv = 1.0f / Math.max(0.001f, angularError);
        return new TargetContext(MathHelper.clamp((float)(worldDistance / 6.0f), (float)0.0f, (float)1.5f), MathHelper.clamp((float)(pitchError / 35.0f), (float)-1.5f, (float)1.5f), angularError, yawError * inv, pitchError * inv, -pitchError * inv, yawError * inv);
    }

    private static Vec3d pickAimPoint(LivingEntity target) {
        Box box = target.getBoundingBox();
        double height = Math.max(0.01, box.getLengthY());
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        double y = box.minY + height * 0.42;
        return new Vec3d(centerX, y, centerZ);
    }

    private static LivingEntity resolveLookTarget(MinecraftClient client) {
        LivingEntity living;
        EntityHitResult entityHit;
        Entity class_12972;
        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult && (class_12972 = (entityHit = (EntityHitResult)hit).getEntity()) instanceof LivingEntity && (living = (LivingEntity)class_12972) != client.player && living.isAlive()) {
            return living;
        }
        return null;
    }

    private static LivingEntity resolveNearestCombatTarget(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return null;
        }
        LivingEntity best = null;
        double bestDistanceSq = 36.0;
        for (Entity entity : client.world.getEntities()) {
            double distSq;
            LivingEntity living;
            if (!(entity instanceof LivingEntity) || (living = (LivingEntity)entity) == client.player || !living.isAlive() || living.isRemoved() || !(living instanceof MobEntity) || !((distSq = client.player.squaredDistanceTo((Entity)living)) < bestDistanceSq)) continue;
            bestDistanceSq = distSq;
            best = living;
        }
        return best;
    }

    private static Rotation rotationTo(Vec3d from, Vec3d to, float baseYaw, float basePitch) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.max(0.001, horiz))));
        return AINewRotationHelper.normalize(new Rotation(targetYaw, targetPitch), new Rotation(baseYaw, basePitch));
    }

    private static float angularDistance(float yawDelta, float pitchDelta) {
        return (float)Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    private static String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private record TargetContext(float worldDistanceNorm, float pitchErrorNorm, float angularErrorDeg, float dirYaw, float dirPitch, float perpYaw, float perpPitch) {
        private static TargetContext free() {
            return new TargetContext(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        }

        private boolean hasAimAxes() {
            return this.angularErrorDeg > 0.05f;
        }
    }

    public record TimeSample(int deltaMs, float yawDeltaDeg, float pitchDeltaDeg, float angularErrorDeg, float forwardMoveDeg, float lateralMoveDeg, float worldDistanceNorm, float pitchErrorNorm, float preAttackNorm, long absMs, boolean hadTarget) {
    }
}

