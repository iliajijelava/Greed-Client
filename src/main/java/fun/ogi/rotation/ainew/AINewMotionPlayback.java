package fun.ogi.rotation.ainew;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.MathHelper;

public final class AINewMotionPlayback {
    private AINewRotationProfile.MouseTrial clip;
    private int frameIndex;
    private long nextFrameNano;
    private long clipStartNano;
    private float clipStartErrorDeg;
    private AimAxes lockedAxes;
    private boolean active;
    private float scaleBias = 1.0f;
    private float handTremorAmp;
    private float handTremorFreqA;
    private float handTremorFreqB;
    private float handTremorPhaseA;
    private float handTremorPhaseB;
    private float handCurveBias;
    private float handSpeedVar;
    private float handDriftFreq;
    private float handDriftPhase;
    private float handSpeedVar2;
    private float handDriftFreq2;
    private float handDriftPhase2;

    public void reset() {
        this.clip = null;
        this.frameIndex = 0;
        this.nextFrameNano = 0L;
        this.clipStartNano = 0L;
        this.clipStartErrorDeg = 0.0f;
        this.lockedAxes = null;
        this.active = false;
        this.scaleBias = 1.0f;
        this.handTremorAmp = 0.0f;
        this.handCurveBias = 0.0f;
        this.handSpeedVar = 0.0f;
        this.handSpeedVar2 = 0.0f;
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isFinished() {
        return !this.active || this.clip == null || this.frameIndex >= this.clip.getPoints().size();
    }

    public float getApproachProgress() {
        if (this.clip == null || this.clip.getPoints().isEmpty()) {
            return 1.0f;
        }
        return MathHelper.clamp((float)((float)this.frameIndex / (float)this.clip.getPoints().size()), (float)0.0f, (float)1.0f);
    }

    public boolean isLookAwayPhase() {
        if (this.clip == null || this.frameIndex <= 0 || this.frameIndex >= this.clip.getPoints().size()) {
            return false;
        }
        AINewRotationProfile.PathNode frame = this.clip.getPoints().get(Math.max(0, this.frameIndex - 1));
        return frame.getForwardMoveDeg() < -0.1f || this.getApproachProgress() < 0.32f && frame.getAngularErrorDeg() > this.clipStartErrorDeg * 0.92f;
    }

    public void start(AINewRotationProfile.MouseTrial selected, long nowNano, float currentErrorDeg, float yawError, float pitchError, boolean repeated) {
        this.reset();
        if (selected == null || selected.getPoints().isEmpty()) {
            return;
        }
        this.clip = selected;
        this.frameIndex = 0;
        this.clipStartNano = nowNano;
        this.clipStartErrorDeg = Math.max(1.0f, Math.max(selected.getStartErrorDeg(), currentErrorDeg));
        this.lockedAxes = AimAxes.fromError(yawError, pitchError, currentErrorDeg);
        this.rollHandSignature(repeated);
        this.active = true;
        this.scheduleNextFrame(nowNano);
    }

    private void rollHandSignature(boolean repeated) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        float intensity = repeated ? 1.0f : 0.8f; 
        float biasRange = repeated ? 0.15f : 0.1f; 
        this.scaleBias = 1.0f + (r.nextFloat() - 0.5f) * biasRange;
        this.handTremorAmp = (0.02f + r.nextFloat() * 0.04f) * intensity; 
        this.handTremorFreqA = 17.0f + r.nextFloat() * 16.0f;
        this.handTremorFreqB = 28.0f + r.nextFloat() * 22.0f;
        this.handTremorPhaseA = r.nextFloat() * 6.2831855f;
        this.handTremorPhaseB = r.nextFloat() * 6.2831855f;
        this.handCurveBias = (r.nextFloat() - 0.5f) * 0.2f * intensity; 
        this.handSpeedVar = 0.04f + r.nextFloat() * 0.06f; 
        this.handDriftFreq = 3.5f + r.nextFloat() * 4.5f;
        this.handDriftPhase = r.nextFloat() * 6.2831855f;
        this.handSpeedVar2 = 0.02f + r.nextFloat() * 0.04f; 
        this.handDriftFreq2 = 8.0f + r.nextFloat() * 7.0f;
        this.handDriftPhase2 = r.nextFloat() * 6.2831855f;
    }

    private float[] applyHumanFeel(float yawDelta, float pitchDelta, AimAxes axes, float elapsedSec, float progress) {
        float motionMag = AINewMotionPlayback.magnitude(yawDelta, pitchDelta);
        if (motionMag < 1.0E-5f) {
            return new float[]{yawDelta, pitchDelta};
        }
        float fwd = yawDelta * axes.dirYaw + pitchDelta * axes.dirPitch;
        float drift = this.handSpeedVar * MathHelper.sin((float)(elapsedSec * this.handDriftFreq + this.handDriftPhase))
            + this.handSpeedVar2 * MathHelper.sin((float)(elapsedSec * this.handDriftFreq2 + this.handDriftPhase2));
        float env = 1.0f + Math.max(0.0f, drift) * 0.6f;
        env = MathHelper.clamp((float)env, (float)1.0f, (float)1.4f);
        float tremor = this.handTremorAmp * (MathHelper.sin((float)(elapsedSec * this.handTremorFreqA + this.handTremorPhaseA)) + 0.5f * MathHelper.sin((float)(elapsedSec * this.handTremorFreqB + this.handTremorPhaseB)));
        float speedGate = 0.3f + 0.7f * MathHelper.clamp((float)(motionMag / 2.2f), (float)0.0f, (float)1.0f);
        float fade = 1.0f - 0.45f * progress;
        float arc = this.handCurveBias * Math.max(0.0f, fwd) * (1.0f - progress) * 1.4f;
        float perp = tremor * speedGate * fade + arc;
        float perpCap = Math.max(0.18f, motionMag * 1.1f + 0.25f);
        perp = MathHelper.clamp((float)perp, (float)(-perpCap), (float)perpCap);
        float yaw = yawDelta * env + perp * axes.perpYaw;
        float pitch = pitchDelta * env + perp * axes.perpPitch;
        return new float[]{yaw, pitch};
    }

    public Step poll(long nowNano, float yawError, float pitchError, float angularError) {
        float pitchDelta;
        float yawDelta;
        AimAxes axes;
        if (!this.active || this.clip == null) {
            return Step.finished();
        }
        if (this.frameIndex >= this.clip.getPoints().size()) {
            this.active = false;
            return Step.finished();
        }
        if (nowNano < this.nextFrameNano) {
            return Step.waiting();
        }
        AINewRotationProfile.PathNode frame = this.clip.getPoints().get(this.frameIndex);
        ++this.frameIndex;
        float progress = this.getApproachProgress();
        boolean flickPhase = AINewMotionPlayback.isFlickPhase(frame, progress);
        AimAxes aimAxes = axes = this.lockedAxes != null ? this.lockedAxes : AimAxes.fromError(yawError, pitchError, angularError);
        if (flickPhase) {
            axes = AimAxes.fromError(yawError, pitchError, angularError);
        }
        float scale = MathHelper.clamp((float)(angularError / this.clipStartErrorDeg), (float)0.72f, (float)1.28f) * this.scaleBias;
        if (AINewMotionPlayback.hasRelativeMotion(frame)) {
            float forward = frame.getForwardMoveDeg() * scale;
            float lateral = frame.getLateralMoveDeg() * scale;
            if (flickPhase && forward < 0.0f) {
                forward = 0.0f;
            }
            yawDelta = forward * axes.dirYaw + lateral * axes.perpYaw;
            pitchDelta = forward * axes.dirPitch + lateral * axes.perpPitch;
        } else if (flickPhase) {
            float moveMag = AINewMotionPlayback.magnitude(frame.getYawDeltaDeg(), frame.getPitchDeltaDeg()) * scale;
            float inv = 1.0f / Math.max(0.001f, angularError);
            yawDelta = yawError * inv * moveMag;
            pitchDelta = pitchError * inv * moveMag;
        } else {
            yawDelta = frame.getYawDeltaDeg() * scale;
            pitchDelta = frame.getPitchDeltaDeg() * scale;
        }
        if (this.frameIndex < this.clip.getPoints().size()) {
            this.scheduleNextFrame(nowNano);
        } else {
            this.active = false;
        }
        float elapsedSec = (float)((double)(nowNano - this.clipStartNano) / 1.0E9);
        float[] human = this.applyHumanFeel(yawDelta, pitchDelta, axes, elapsedSec, progress);
        yawDelta = human[0] * (1.0f + (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.03f);
        pitchDelta = human[1] * (1.0f + (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.03f);
        return Step.move(yawDelta, pitchDelta);
    }

    private void scheduleNextFrame(long nowNano) {
        if (this.clip == null || this.frameIndex >= this.clip.getPoints().size()) {
            return;
        }
        int waitMs = Math.max(1, this.clip.getPoints().get(this.frameIndex).getDtMs());
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long jitterNano = (long)((r.nextDouble() - 0.5) * 8500000.0);
        if (r.nextFloat() < 0.08f) {
            jitterNano += (long)(r.nextDouble() * 12000000.0);
        }
        long scheduled = nowNano + (long)waitMs * 1000000L + jitterNano;
        this.nextFrameNano = Math.max(nowNano + 1000000L, scheduled);
    }

    private static boolean hasRelativeMotion(AINewRotationProfile.PathNode frame) {
        return Math.abs(frame.getForwardMoveDeg()) > 0.02f || Math.abs(frame.getLateralMoveDeg()) > 0.02f;
    }

    private static boolean isFlickPhase(AINewRotationProfile.PathNode frame, float progress) {
        if (progress >= 0.28f) {
            return true;
        }
        if (AINewMotionPlayback.hasRelativeMotion(frame) && frame.getForwardMoveDeg() > 0.08f) {
            return true;
        }
        return AINewMotionPlayback.magnitude(frame.getYawDeltaDeg(), frame.getPitchDeltaDeg()) >= 1.2f && frame.getForwardMoveDeg() >= -0.05f;
    }

    private static float magnitude(float yawDelta, float pitchDelta) {
        return (float)Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    public static AINewRotationProfile.MouseTrial pickPreAttackClip(List<AINewRotationProfile.MouseTrial> clips, float currentErrorDeg, float yawErrorDeg) {
        return AINewMotionPlayback.pickClip(clips, currentErrorDeg, yawErrorDeg, null, null, null, null, System.currentTimeMillis());
    }

    public static AINewRotationProfile.MouseTrial pickClip(List<AINewRotationProfile.MouseTrial> clips, float currentErrorDeg, float yawErrorDeg, java.util.Collection<AINewRotationProfile.MouseTrial> recentClips, java.util.Map<AINewRotationProfile.MouseTrial, Long> cooldownUntilMs, AINewRotationProfile.MouseTrial lastClip, java.util.Collection<AINewRotationProfile.MouseTrial> blockedFollowUps, long nowMs) {
        if (clips == null || clips.isEmpty()) {
            return null;
        }
        List<AINewRotationProfile.MouseTrial> ranked = AINewMotionPlayback.collectRankedClips(clips);
        if (ranked.isEmpty()) {
            return null;
        }
        ranked.sort(Comparator.comparingDouble(candidate -> AINewMotionPlayback.clipDistance(candidate, currentErrorDeg, yawErrorDeg)));
        if (ranked.size() == 1) {
            return ranked.get(0);
        }
        ArrayList<AINewRotationProfile.MouseTrial> tier = new ArrayList<AINewRotationProfile.MouseTrial>(ranked.size());
        for (AINewRotationProfile.MouseTrial candidate : ranked) {
            if (candidate == lastClip || AINewMotionPlayback.contains(recentClips, candidate) || AINewMotionPlayback.isOnCooldown(cooldownUntilMs, candidate, nowMs) || AINewMotionPlayback.contains(blockedFollowUps, candidate)) continue;
            tier.add(candidate);
        }
        if (tier.isEmpty()) {
            for (AINewRotationProfile.MouseTrial candidate : ranked) {
                if (candidate == lastClip || AINewMotionPlayback.isOnCooldown(cooldownUntilMs, candidate, nowMs)) continue;
                tier.add(candidate);
            }
        }
        if (tier.isEmpty()) {
            for (AINewRotationProfile.MouseTrial candidate : ranked) {
                if (candidate == lastClip) continue;
                tier.add(candidate);
            }
        }
        if (tier.isEmpty()) {
            tier.addAll(ranked);
        }
        return AINewMotionPlayback.weightedPick(tier);
    }

    private static boolean contains(java.util.Collection<AINewRotationProfile.MouseTrial> collection, AINewRotationProfile.MouseTrial clip) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }
        for (AINewRotationProfile.MouseTrial existing : collection) {
            if (existing == clip) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOnCooldown(java.util.Map<AINewRotationProfile.MouseTrial, Long> cooldownUntilMs, AINewRotationProfile.MouseTrial clip, long nowMs) {
        if (cooldownUntilMs == null || cooldownUntilMs.isEmpty()) {
            return false;
        }
        Long until = cooldownUntilMs.get(clip);
        return until != null && until > nowMs;
    }

    private static AINewRotationProfile.MouseTrial weightedPick(List<AINewRotationProfile.MouseTrial> pool) {
        int n = Math.min(3, pool.size());
        if (n <= 1) {
            return pool.get(0);
        }
        double[] weights = new double[n];
        double total = 0.0;
        for (int i = 0; i < n; ++i) {
            weights[i] = 1.0 / (1.0 + (double)i * 1.1);
            total += weights[i];
        }
        double r = ThreadLocalRandom.current().nextDouble() * total;
        double acc = 0.0;
        for (int i = 0; i < n; ++i) {
            acc += weights[i];
            if (r <= acc) {
                return pool.get(i);
            }
        }
        return pool.get(0);
    }

    private static List<AINewRotationProfile.MouseTrial> collectRankedClips(List<AINewRotationProfile.MouseTrial> clips) {
        ArrayList<AINewRotationProfile.MouseTrial> ranked = new ArrayList<AINewRotationProfile.MouseTrial>();
        for (AINewRotationProfile.MouseTrial clip : clips) {
            if (clip == null || !clip.isSuccess() || !clip.isPreAttackClip() || clip.getPoints().isEmpty() || AINewRotationProfile.trialPeakMoveDeg(clip) < 0.35f) continue;
            ranked.add(clip);
        }
        if (ranked.isEmpty()) {
            for (AINewRotationProfile.MouseTrial clip : clips) {
                if (clip == null || !clip.isSuccess() || clip.getPoints().isEmpty() || AINewRotationProfile.trialPeakMoveDeg(clip) < 0.35f) continue;
                ranked.add(clip);
            }
        }
        return ranked;
    }

    private static double clipDistance(AINewRotationProfile.MouseTrial clip, float currentErrorDeg, float yawErrorDeg) {
        float startErr = clip.getStartErrorDeg();
        float endErr = clip.getEndErrorDeg();
        double errorGap = Math.abs(startErr - currentErrorDeg);
        double finishGap = Math.abs(endErr - Math.min(currentErrorDeg, 3.0f));
        double yawNeed = Math.abs(yawErrorDeg) >= 55.0f ? 0.0 : (double)Math.abs(yawErrorDeg) * 0.01;
        return errorGap * 1.15 + finishGap * 0.65 + yawNeed - (double)AINewRotationProfile.trialPreAttackScore(clip) * 2.5;
    }

    private static final class AimAxes {
        private final float dirYaw;
        private final float dirPitch;
        private final float perpYaw;
        private final float perpPitch;

        private AimAxes(float dirYaw, float dirPitch, float perpYaw, float perpPitch) {
            this.dirYaw = dirYaw;
            this.dirPitch = dirPitch;
            this.perpYaw = perpYaw;
            this.perpPitch = perpPitch;
        }

        private static AimAxes fromError(float yawError, float pitchError, float angularError) {
            float inv = 1.0f / Math.max(0.001f, angularError);
            return new AimAxes(yawError * inv, pitchError * inv, -pitchError * inv, yawError * inv);
        }
    }

    public static final class Step {
        private final float yawDelta;
        private final float pitchDelta;
        private final boolean waiting;
        private final boolean finished;

        private Step(float yawDelta, float pitchDelta, boolean waiting, boolean finished) {
            this.yawDelta = yawDelta;
            this.pitchDelta = pitchDelta;
            this.waiting = waiting;
            this.finished = finished;
        }

        public static Step waiting() {
            return new Step(0.0f, 0.0f, true, false);
        }

        public static Step move(float yawDelta, float pitchDelta) {
            return new Step(yawDelta, pitchDelta, false, false);
        }

        public static Step finished() {
            return new Step(0.0f, 0.0f, false, true);
        }

        public float getYawDelta() {
            return this.yawDelta;
        }

        public float getPitchDelta() {
            return this.pitchDelta;
        }

        public boolean isWaiting() {
            return this.waiting;
        }

        public boolean isFinished() {
            return this.finished;
        }

        public boolean hasMovement() {
            return !this.waiting && !this.finished;
        }
    }
}

