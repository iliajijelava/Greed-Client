package fun.ogi.rotation.ainew;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.MathHelper;

public final class AINewMotionClipBuilder {
    private static final int MAX_CLIPS = 100;
    private static final int MAX_FRAMES_PER_CLIP = 48;
    private static final long PRE_ATTACK_MS = 3000L;
    private static final long POST_ATTACK_MS = 120L;

    private AINewMotionClipBuilder() {
    }

    public static List<AINewRotationProfile.MouseTrial> buildClips(List<AINewCombatTrainingSession.TimeSample> timeline, List<Long> attackTimesMs) {
        if (timeline == null || timeline.size() < 2 || attackTimesMs == null || attackTimesMs.isEmpty()) {
            return List.of();
        }
        ArrayList<AINewRotationProfile.MouseTrial> clips = new ArrayList<AINewRotationProfile.MouseTrial>();
        for (Long attackMs : attackTimesMs) {
            if (attackMs == null) continue;
            AINewMotionClipBuilder.addClipIfValid(clips, AINewMotionClipBuilder.extractPreAttackWindow(timeline, attackMs));
        }
        return AINewMotionClipBuilder.compactClips(clips);
    }

    private static boolean addClipIfValid(List<AINewRotationProfile.MouseTrial> clips, AINewRotationProfile.MouseTrial clip) {
        if (clip == null || !clip.isPreAttackClip() || clip.getPoints().size() < 2) {
            return false;
        }
        if (AINewRotationProfile.trialPeakMoveDeg(clip) < 0.2f) {
            return false;
        }
        clips.add(clip);
        return true;
    }

    private static AINewRotationProfile.MouseTrial extractPreAttackWindow(List<AINewCombatTrainingSession.TimeSample> timeline, long attackMs) {
        long startMs = attackMs - 3000L;
        long endMs = attackMs + 120L;
        List<AINewCombatTrainingSession.TimeSample> window = new ArrayList<AINewCombatTrainingSession.TimeSample>();
        for (AINewCombatTrainingSession.TimeSample sample : timeline) {
            if (sample.absMs() < startMs || sample.absMs() > endMs) continue;
            window.add(sample);
        }
        if (window.size() < 4) {
            return null;
        }
        if (!AINewMotionClipBuilder.isValidPreAttackSequence(window, attackMs)) {
            return null;
        }
        if (window.size() > 48) {
            window = AINewMotionClipBuilder.downsample(window, 48);
        }
        ArrayList<AINewRotationProfile.PathNode> points = new ArrayList<AINewRotationProfile.PathNode>(window.size());
        float totalPath = 0.0f;
        float totalYawAbs = 0.0f;
        float maxLateral = 0.0f;
        float startError = window.get(0).angularErrorDeg();
        float endError = window.get(window.size() - 1).angularErrorDeg();
        float lateralAccum = 0.0f;
        for (AINewCombatTrainingSession.TimeSample sample : window) {
            float yaw = sample.yawDeltaDeg();
            float pitch = AINewMotionClipBuilder.sanitizePitchDelta(yaw, sample.pitchDeltaDeg(), sample.angularErrorDeg());
            totalPath += AINewMotionClipBuilder.moveDeg(yaw, pitch);
            totalYawAbs += Math.abs(yaw);
            maxLateral = Math.max(maxLateral, Math.abs(lateralAccum += sample.lateralMoveDeg()));
            points.add(new AINewRotationProfile.PathNode(yaw, pitch, sample.angularErrorDeg(), sample.forwardMoveDeg(), sample.lateralMoveDeg(), sample.deltaMs(), sample.worldDistanceNorm(), sample.pitchErrorNorm(), sample.preAttackNorm()));
        }
        long durationMs = Math.max(1L, window.get(window.size() - 1).absMs() - window.get(0).absMs());
        float distance = Math.max(startError, totalPath / (float)Math.max(1, points.size() - 1));
        float curvature = MathHelper.clamp((float)(maxLateral / Math.max(1.0f, distance)), (float)0.0f, (float)1.5f);
        return new AINewRotationProfile.MouseTrial(true, durationMs, startError, endError, Math.max(distance, totalPath), curvature, totalYawAbs, true, List.copyOf(points));
    }

    private static boolean isValidPreAttackSequence(List<AINewCombatTrainingSession.TimeSample> window, long attackMs) {
        float startError = window.get(0).angularErrorDeg();
        float endError = window.get(window.size() - 1).angularErrorDeg();
        float totalYawAbs = 0.0f;
        float totalMove = 0.0f;
        float bestPreAttack = 0.0f;
        int lookAwayFrames = 0;
        for (int i = 0; i < window.size(); ++i) {
            long delta;
            AINewCombatTrainingSession.TimeSample sample = window.get(i);
            totalYawAbs += Math.abs(sample.yawDeltaDeg());
            totalMove += AINewMotionClipBuilder.moveDeg(sample);
            if (sample.absMs() <= attackMs && (delta = attackMs - sample.absMs()) >= 0L && delta <= 3000L) {
                bestPreAttack = Math.max(bestPreAttack, 1.0f - (float)delta / 3000.0f);
            }
            if (sample.forwardMoveDeg() < -0.12f) {
                ++lookAwayFrames;
            }
            if (i <= 0 || !(sample.angularErrorDeg() > window.get(i - 1).angularErrorDeg() + 0.8f)) continue;
            ++lookAwayFrames;
        }
        if (bestPreAttack < 0.1f) {
            return false;
        }
        if (endError > startError * 0.78f && endError > 4.0f) {
            return false;
        }
        if (endError > 12.0f && endError > startError * 0.55f) {
            return false;
        }
        if (totalYawAbs > 160.0f && endError > 8.0f) {
            return false;
        }
        if (startError < 3.0f && lookAwayFrames == 0 && endError < 2.5f && totalMove < 6.0f) {
            return false;
        }
        return totalMove >= 1.5f && (startError >= 2.5f || lookAwayFrames > 0 || totalYawAbs >= 8.0f);
    }

    private static float sanitizePitchDelta(float yawDelta, float pitchDelta, float angularError) {
        if (Math.abs(yawDelta) > 1.0f && Math.abs(pitchDelta) < 0.35f) {
            return 0.0f;
        }
        if (Math.abs(pitchDelta) > angularError * 0.55f) {
            return Math.copySign(angularError * 0.35f, pitchDelta);
        }
        return pitchDelta;
    }

    private static List<AINewCombatTrainingSession.TimeSample> downsample(List<AINewCombatTrainingSession.TimeSample> window, int maxFrames) {
        int i;
        if (window.size() <= maxFrames) {
            return window;
        }
        boolean[] keep = new boolean[window.size()];
        keep[0] = true;
        keep[window.size() - 1] = true;
        int lead = Math.min(14, window.size());
        for (i = 1; i < lead; ++i) {
            keep[i] = true;
        }
        for (i = 1; i < window.size() - 1; ++i) {
            AINewCombatTrainingSession.TimeSample sample = window.get(i);
            if (!(AINewMotionClipBuilder.moveDeg(sample) >= 1.8f) && !(sample.forwardMoveDeg() < -0.1f) && !(sample.preAttackNorm() > 0.25f)) continue;
            keep[i] = true;
            if (i <= 0) continue;
            keep[i - 1] = true;
        }
        ArrayList<AINewCombatTrainingSession.TimeSample> result = new ArrayList<AINewCombatTrainingSession.TimeSample>(maxFrames);
        for (int i2 = 0; i2 < window.size() && result.size() < maxFrames; ++i2) {
            if (!keep[i2]) continue;
            result.add(window.get(i2));
        }
        if (result.size() < 2) {
            return List.of(window.get(0), window.get(window.size() - 1));
        }
        return result;
    }

    public static List<AINewRotationProfile.MouseTrial> compactClips(List<AINewRotationProfile.MouseTrial> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        ArrayList<AINewRotationProfile.MouseTrial> ranked = new ArrayList<AINewRotationProfile.MouseTrial>(source);
        ranked.sort((a, b) -> Float.compare(AINewMotionClipBuilder.scoreClip(b), AINewMotionClipBuilder.scoreClip(a)));
        ArrayList<AINewRotationProfile.MouseTrial> kept = new ArrayList<AINewRotationProfile.MouseTrial>(Math.min(100, ranked.size()));
        for (int i = 0; i < ranked.size() && kept.size() < 100; ++i) {
            AINewRotationProfile.MouseTrial compact = AINewRotationProfile.compactTrial((AINewRotationProfile.MouseTrial)ranked.get(i));
            if (compact == null) continue;
            kept.add(compact);
        }
        return List.copyOf(kept);
    }

    private static float scoreClip(AINewRotationProfile.MouseTrial clip) {
        float peak = AINewRotationProfile.trialPeakMoveDeg(clip);
        if (peak < 0.2f) {
            return -1000.0f;
        }
        float closeBonus = Math.max(0.0f, clip.getStartErrorDeg() - clip.getEndErrorDeg());
        return peak + AINewRotationProfile.trialPreAttackScore(clip) * 6.0f + (AINewRotationProfile.trialHasLookAwayPhase(clip) ? 3.0f : 0.0f) + closeBonus * 0.35f;
    }

    private static float moveDeg(AINewCombatTrainingSession.TimeSample sample) {
        return AINewMotionClipBuilder.moveDeg(sample.yawDeltaDeg(), sample.pitchDeltaDeg());
    }

    private static float moveDeg(float yawDelta, float pitchDelta) {
        return (float)Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }
}

