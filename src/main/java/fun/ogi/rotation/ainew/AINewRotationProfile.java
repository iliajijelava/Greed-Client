package fun.ogi.rotation.ainew;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public final class AINewRotationProfile {
    public static final int SCHEMA_VERSION = 2;
    public static final int MAX_STORED_CLIPS = 100;
    public static final int MAX_FRAMES_PER_CLIP = 48;
    private final String name;
    private final long createdAtMs;
    private final long trainingDurationMs;
    private final int hitCount;
    private final float averageReactionMs;
    private final float averageSpeedDegPerSec;
    private final List<MouseTrial> motionClips;
    private final boolean yawDominantStyle;

    public AINewRotationProfile(String name, long createdAtMs, long trainingDurationMs, int hitCount, float averageReactionMs, float averageSpeedDegPerSec, List<MouseTrial> motionClips) {
        this.name = name;
        this.createdAtMs = createdAtMs;
        this.trainingDurationMs = trainingDurationMs;
        this.hitCount = hitCount;
        this.averageReactionMs = averageReactionMs;
        this.averageSpeedDegPerSec = averageSpeedDegPerSec;
        this.motionClips = AINewRotationProfile.compactClips(motionClips == null ? List.of() : motionClips);
        this.yawDominantStyle = AINewRotationProfile.computeYawDominantStyle(this.motionClips);
    }

    public String getName() {
        return this.name;
    }

    public long getCreatedAtMs() {
        return this.createdAtMs;
    }

    public long getTrainingDurationMs() {
        return this.trainingDurationMs;
    }

    public int getHitCount() {
        return this.hitCount;
    }

    public float getAverageReactionMs() {
        return this.averageReactionMs;
    }

    public float getAverageSpeedDegPerSec() {
        return this.averageSpeedDegPerSec;
    }

    @Deprecated
    public float getAverageSpeedPxPerSec() {
        return this.averageSpeedDegPerSec;
    }

    public List<MouseTrial> getMotionClips() {
        return this.motionClips;
    }

    @Deprecated
    public List<MouseTrial> getDatasets() {
        return this.motionClips;
    }

    public boolean hasMotionClips() {
        return !this.motionClips.isEmpty();
    }

    public boolean isYawDominantStyle() {
        return this.yawDominantStyle;
    }

    public String getShortSummary() {
        return this.name + " (" + this.hitCount + " hits, " + this.motionClips.size() + " clips)";
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("schemaVersion", (Number)2);
        obj.addProperty("name", this.name);
        obj.addProperty("createdAtMs", (Number)this.createdAtMs);
        obj.addProperty("trainingDurationMs", (Number)this.trainingDurationMs);
        obj.addProperty("hitCount", (Number)this.hitCount);
        obj.addProperty("averageReactionMs", (Number)Float.valueOf(this.averageReactionMs));
        obj.addProperty("averageSpeedDegPerSec", (Number)Float.valueOf(this.averageSpeedDegPerSec));
        obj.addProperty("yawDominantStyle", Boolean.valueOf(this.yawDominantStyle));
        JsonArray clipArray = new JsonArray();
        for (MouseTrial clip : this.motionClips) {
            clipArray.add((JsonElement)clip.toCompactJson());
        }
        obj.add("motionClips", (JsonElement)clipArray);
        obj.add("datasets", (JsonElement)clipArray);
        return obj;
    }

    public static AINewRotationProfile fromJson(JsonObject obj) {
        String name;
        if (obj == null) {
            return null;
        }
        String string = name = obj.has("name") ? obj.get("name").getAsString() : null;
        if (name == null || name.isBlank()) {
            return null;
        }
        ArrayList<MouseTrial> clips = new ArrayList<MouseTrial>();
        if (obj.has("motionClips") && obj.get("motionClips").isJsonArray()) {
            AINewRotationProfile.readClips(obj.getAsJsonArray("motionClips"), clips);
        } else if (obj.has("datasets") && obj.get("datasets").isJsonArray()) {
            AINewRotationProfile.readClips(obj.getAsJsonArray("datasets"), clips);
        }
        return new AINewRotationProfile(name, obj.has("createdAtMs") ? obj.get("createdAtMs").getAsLong() : System.currentTimeMillis(), obj.has("trainingDurationMs") ? obj.get("trainingDurationMs").getAsLong() : 60000L, obj.has("hitCount") ? obj.get("hitCount").getAsInt() : 0, obj.has("averageReactionMs") ? obj.get("averageReactionMs").getAsFloat() : 220.0f, obj.has("averageSpeedDegPerSec") ? obj.get("averageSpeedDegPerSec").getAsFloat() : (obj.has("averageSpeedPxPerSec") ? obj.get("averageSpeedPxPerSec").getAsFloat() : 120.0f), clips);
    }

    private static void readClips(JsonArray array, List<MouseTrial> clips) {
        for (JsonElement element : array) {
            MouseTrial clip;
            if (!element.isJsonObject() || (clip = MouseTrial.fromJson(element.getAsJsonObject())) == null) continue;
            clips.add(clip);
        }
    }

    public static AINewRotationProfile fallback() {
        return new AINewRotationProfile("Fallback", System.currentTimeMillis(), 60000L, 0, 220.0f, 120.0f, List.of());
    }

    public static List<MouseTrial> compactClips(List<MouseTrial> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        ArrayList<MouseTrial> ranked = new ArrayList<MouseTrial>();
        for (MouseTrial clip : source) {
            if (clip == null || !clip.isSuccess() || !clip.isPreAttackClip() || clip.getPoints().isEmpty()) continue;
            ranked.add(clip);
        }
        if (ranked.isEmpty()) {
            for (MouseTrial clip : source) {
                if (clip == null || !clip.isSuccess() || clip.getPoints().isEmpty()) continue;
                ranked.add(clip);
            }
        }
        ranked.sort((a, b) -> Float.compare(AINewRotationProfile.scoreClip(b), AINewRotationProfile.scoreClip(a)));
        ArrayList<MouseTrial> kept = new ArrayList<MouseTrial>(Math.min(100, ranked.size()));
        for (int i = 0; i < ranked.size() && kept.size() < 100; ++i) {
            MouseTrial compact = AINewRotationProfile.compactTrial((MouseTrial)ranked.get(i));
            if (compact == null) continue;
            kept.add(compact);
        }
        return List.copyOf(kept);
    }

    public static MouseTrial compactTrial(MouseTrial trial) {
        int i;
        int i2;
        List<PathNode> points = trial.getPoints();
        if (points.isEmpty()) {
            return null;
        }
        if (points.size() <= 48) {
            return trial;
        }
        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;
        int leadLimit = Math.min(14, points.size());
        for (i2 = 1; i2 < leadLimit; ++i2) {
            keep[i2] = true;
        }
        for (i2 = 1; i2 < points.size() - 1; ++i2) {
            Object node = points.get(i2);
            if (!((PathNode)node).isFlickFrame() && !(((PathNode)node).getForwardMoveDeg() < -0.1f)) continue;
            keep[i2] = true;
            if (i2 > 0) {
                keep[i2 - 1] = true;
            }
            if (i2 + 1 >= points.size()) continue;
            keep[i2 + 1] = true;
        }
        int keptCount = 0;
        for (boolean value : keep) {
            if (!value) continue;
            ++keptCount;
        }
        if (keptCount < 48) {
            int stride = Math.max(1, points.size() / Math.max(1, 48 - keptCount));
            for (i = 1; i < points.size() - 1; i += stride) {
                if (keep[i]) continue;
                keep[i] = true;
                if (++keptCount >= 48) break;
            }
        }
        ArrayList<PathNode> compact = new ArrayList<PathNode>(48);
        for (i = 0; i < points.size() && compact.size() < 48; ++i) {
            if (!keep[i]) continue;
            compact.add(points.get(i));
        }
        if (compact.size() < 2) {
            compact.clear();
            compact.add(points.get(0));
            compact.add(points.get(points.size() - 1));
        }
        float endError = ((PathNode)compact.get(compact.size() - 1)).getAngularErrorDeg();
        return new MouseTrial(trial.isSuccess(), trial.getDurationMs(), trial.getStartErrorDeg(), endError, trial.getPathLengthDeg(), trial.getCurvature(), trial.getTotalYawAbs(), trial.isPreAttackClip(), List.copyOf(compact));
    }

    private static float scoreClip(MouseTrial clip) {
        float peak = AINewRotationProfile.trialPeakMoveDeg(clip);
        if (peak < 0.2f) {
            return -1000.0f;
        }
        return peak + AINewRotationProfile.trialPreAttackScore(clip) * 4.0f + (AINewRotationProfile.trialHasLookAwayPhase(clip) ? 4.0f : 0.0f);
    }

    private static boolean computeYawDominantStyle(List<MouseTrial> clips) {
        float yaw = 0.0f;
        float pitch = 0.0f;
        for (MouseTrial clip : clips) {
            yaw += AINewRotationProfile.trialYawMoveSum(clip);
            pitch += AINewRotationProfile.trialPitchMoveSum(clip);
        }
        if (yaw <= 0.5f) {
            return true;
        }
        return pitch < yaw * 0.55f;
    }

    public static float trialPeakMoveDeg(MouseTrial trial) {
        if (trial == null) {
            return 0.0f;
        }
        float peak = 0.0f;
        for (PathNode point : trial.getPoints()) {
            peak = Math.max(peak, AINewRotationProfile.pointMove(point));
        }
        return peak;
    }

    public static boolean trialHasLookAwayPhase(MouseTrial trial) {
        if (trial == null || trial.getPoints().size() < 3) {
            return false;
        }
        List<PathNode> points = trial.getPoints();
        for (int i = 1; i < points.size(); ++i) {
            if (points.get(i).getAngularErrorDeg() > points.get(i - 1).getAngularErrorDeg() + 1.0f) {
                return true;
            }
            if (!(points.get(i).getForwardMoveDeg() < -0.35f)) continue;
            return true;
        }
        return false;
    }

    public static float trialPreAttackScore(MouseTrial trial) {
        if (trial == null) {
            return 0.0f;
        }
        float best = 0.0f;
        for (PathNode point : trial.getPoints()) {
            best = Math.max(best, point.getPreAttackNorm());
        }
        return best;
    }

    public static float trialYawMoveSum(MouseTrial trial) {
        if (trial == null) {
            return 0.0f;
        }
        float sum = 0.0f;
        for (PathNode point : trial.getPoints()) {
            sum += Math.abs(point.getYawDeltaDeg());
        }
        return sum;
    }

    public static float trialPitchMoveSum(MouseTrial trial) {
        if (trial == null) {
            return 0.0f;
        }
        float sum = 0.0f;
        for (PathNode point : trial.getPoints()) {
            sum += Math.abs(point.getPitchDeltaDeg());
        }
        return sum;
    }

    private static float pointMove(PathNode point) {
        return (float)Math.sqrt(point.getYawDeltaDeg() * point.getYawDeltaDeg() + point.getPitchDeltaDeg() * point.getPitchDeltaDeg());
    }

    public static final class MouseTrial {
        private final boolean success;
        private final long durationMs;
        private final float startErrorDeg;
        private final float endErrorDeg;
        private final float pathLengthDeg;
        private final float curvature;
        private final float totalYawAbs;
        private final boolean preAttackClip;
        private final List<PathNode> points;

        public MouseTrial(boolean success, long durationMs, float startErrorDeg, float endErrorDeg, float pathLengthDeg, float curvature, float totalYawAbs, boolean preAttackClip, List<PathNode> points) {
            this.success = success;
            this.durationMs = durationMs;
            this.startErrorDeg = startErrorDeg;
            this.endErrorDeg = endErrorDeg;
            this.pathLengthDeg = pathLengthDeg;
            this.curvature = curvature;
            this.totalYawAbs = totalYawAbs;
            this.preAttackClip = preAttackClip;
            this.points = points == null ? List.of() : List.copyOf(points);
        }

        public MouseTrial(boolean success, long durationMs, float startErrorDeg, float pathLengthDeg, float curvature, List<PathNode> points) {
            this(success, durationMs, startErrorDeg, points.isEmpty() ? startErrorDeg : points.get(points.size() - 1).getAngularErrorDeg(), pathLengthDeg, curvature, 0.0f, false, points);
        }

        public boolean isSuccess() {
            return this.success;
        }

        public long getDurationMs() {
            return this.durationMs;
        }

        @Deprecated
        public long getReactionMs() {
            return this.durationMs;
        }

        public float getStartErrorDeg() {
            return this.startErrorDeg;
        }

        public float getEndErrorDeg() {
            return this.endErrorDeg;
        }

        public float getTotalYawAbs() {
            return this.totalYawAbs;
        }

        public boolean isPreAttackClip() {
            return this.preAttackClip;
        }

        public float getPathLengthDeg() {
            return this.pathLengthDeg;
        }

        public float getCurvature() {
            return this.curvature;
        }

        public List<PathNode> getPoints() {
            return this.points;
        }

        public JsonObject toCompactJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("success", Boolean.valueOf(this.success));
            obj.addProperty("durationMs", (Number)this.durationMs);
            obj.addProperty("reactionMs", (Number)this.durationMs);
            obj.addProperty("startErrorDeg", (Number)Float.valueOf(this.startErrorDeg));
            obj.addProperty("endErrorDeg", (Number)Float.valueOf(this.endErrorDeg));
            obj.addProperty("preAttackClip", Boolean.valueOf(this.preAttackClip));
            JsonArray frameArray = new JsonArray();
            for (PathNode point : this.points) {
                frameArray.add((JsonElement)point.toCompactJson());
            }
            obj.add("points", (JsonElement)frameArray);
            return obj;
        }

        public static MouseTrial fromJson(JsonObject obj) {
            if (obj == null) {
                return null;
            }
            List<PathNode> points = new ArrayList<PathNode>();
            if (obj.has("points") && obj.get("points").isJsonArray()) {
                JsonArray array = obj.getAsJsonArray("points");
                for (JsonElement element : array) {
                    PathNode point;
                    if (!element.isJsonObject() || (point = PathNode.fromJson(element.getAsJsonObject())) == null) continue;
                    points.add(point);
                }
            }
            long durationMs = obj.has("durationMs") ? obj.get("durationMs").getAsLong() : (obj.has("reactionMs") ? obj.get("reactionMs").getAsLong() : 1L);
            float startError = obj.has("startErrorDeg") ? obj.get("startErrorDeg").getAsFloat() : (points.isEmpty() ? 1.0f : ((PathNode)points.get(0)).getAngularErrorDeg());
            float endError = obj.has("endErrorDeg") ? obj.get("endErrorDeg").getAsFloat() : (points.isEmpty() ? startError : ((PathNode)points.get(points.size() - 1)).getAngularErrorDeg());
            boolean preAttack;
            if (obj.has("preAttackClip")) {
                preAttack = obj.get("preAttackClip").getAsBoolean();
            } else if (endError < startError * 0.68f) {
                MouseTrial trial = new MouseTrial(true, durationMs, startError, endError, startError, 0.2f, 0.0f, false, points);
                preAttack = AINewRotationProfile.trialPreAttackScore(trial) > 0.12f;
            } else {
                preAttack = false;
            }
            return new MouseTrial(obj.has("success") ? obj.get("success").getAsBoolean() : true, durationMs, startError, endError, obj.has("pathLengthDeg") ? obj.get("pathLengthDeg").getAsFloat() : startError, obj.has("curvature") ? obj.get("curvature").getAsFloat() : 0.2f, obj.has("totalYawAbs") ? obj.get("totalYawAbs").getAsFloat() : 0.0f, preAttack, points);
        }
    }

    public static final class PathNode {
        private final float yawDeltaDeg;
        private final float pitchDeltaDeg;
        private final float angularErrorDeg;
        private final float forwardMoveDeg;
        private final float lateralMoveDeg;
        private final int dtMs;
        private final float worldDistanceNorm;
        private final float pitchErrorNorm;
        private final float preAttackNorm;

        public PathNode(float yawDeltaDeg, float pitchDeltaDeg, float angularErrorDeg, float forwardMoveDeg, float lateralMoveDeg, int dtMs) {
            this(yawDeltaDeg, pitchDeltaDeg, angularErrorDeg, forwardMoveDeg, lateralMoveDeg, dtMs, 0.0f, 0.0f, 0.0f);
        }

        public PathNode(float yawDeltaDeg, float pitchDeltaDeg, float angularErrorDeg, float forwardMoveDeg, float lateralMoveDeg, int dtMs, float worldDistanceNorm, float pitchErrorNorm, float preAttackNorm) {
            this.yawDeltaDeg = yawDeltaDeg;
            this.pitchDeltaDeg = pitchDeltaDeg;
            this.angularErrorDeg = angularErrorDeg;
            this.forwardMoveDeg = forwardMoveDeg;
            this.lateralMoveDeg = lateralMoveDeg;
            this.dtMs = Math.max(1, dtMs);
            this.worldDistanceNorm = worldDistanceNorm;
            this.pitchErrorNorm = pitchErrorNorm;
            this.preAttackNorm = preAttackNorm;
        }

        public PathNode(float time, float forward, float lateral, float worldDistanceNorm, float pitchErrorNorm, float yawDeltaDeg, float pitchDeltaDeg, float angularErrorDeg, float forwardMoveDeg, float lateralMoveDeg, int dtMs, float approachProgress, float lateralOffsetNorm, float preAttackNorm) {
            this(yawDeltaDeg, pitchDeltaDeg, angularErrorDeg, forwardMoveDeg, lateralMoveDeg, dtMs, worldDistanceNorm, pitchErrorNorm, preAttackNorm);
        }

        public float getYawDeltaDeg() {
            return this.yawDeltaDeg;
        }

        public float getPitchDeltaDeg() {
            return this.pitchDeltaDeg;
        }

        public float getAngularErrorDeg() {
            return this.angularErrorDeg;
        }

        public float getForwardMoveDeg() {
            return this.forwardMoveDeg;
        }

        public float getLateralMoveDeg() {
            return this.lateralMoveDeg;
        }

        public int getDtMs() {
            return this.dtMs;
        }

        public float getTickDtSec() {
            return (float)this.dtMs / 1000.0f;
        }

        public float getPreAttackNorm() {
            return this.preAttackNorm;
        }

        public boolean isFlickFrame() {
            float move = PathNode.pointMove(this);
            float speed = move / Math.max(0.001f, (float)this.dtMs / 1000.0f);
            return speed >= 45.0f || move >= 2.4f || Math.abs(this.lateralMoveDeg) >= 1.5f;
        }

        public JsonObject toCompactJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("dtMs", (Number)this.dtMs);
            obj.addProperty("yawDeltaDeg", (Number)Float.valueOf(this.yawDeltaDeg));
            obj.addProperty("pitchDeltaDeg", (Number)Float.valueOf(this.pitchDeltaDeg));
            obj.addProperty("angularErrorDeg", (Number)Float.valueOf(this.angularErrorDeg));
            obj.addProperty("forwardMoveDeg", (Number)Float.valueOf(this.forwardMoveDeg));
            obj.addProperty("preAttackNorm", (Number)Float.valueOf(this.preAttackNorm));
            return obj;
        }

        public static PathNode fromJson(JsonObject obj) {
            if (obj == null) {
                return null;
            }
            float yawDelta = obj.has("yawDeltaDeg") ? obj.get("yawDeltaDeg").getAsFloat() : 0.0f;
            float pitchDelta = obj.has("pitchDeltaDeg") ? obj.get("pitchDeltaDeg").getAsFloat() : 0.0f;
            int dtMs = 16;
            if (obj.has("dtMs")) {
                dtMs = obj.get("dtMs").getAsInt();
            } else if (obj.has("tickDtSec")) {
                dtMs = Math.max(1, Math.round(obj.get("tickDtSec").getAsFloat() * 1000.0f));
            }
            return new PathNode(yawDelta, pitchDelta, obj.has("angularErrorDeg") ? obj.get("angularErrorDeg").getAsFloat() : PathNode.pointMove(yawDelta, pitchDelta), obj.has("forwardMoveDeg") ? obj.get("forwardMoveDeg").getAsFloat() : 0.0f, obj.has("lateralMoveDeg") ? obj.get("lateralMoveDeg").getAsFloat() : 0.0f, dtMs, obj.has("worldDistanceNorm") ? obj.get("worldDistanceNorm").getAsFloat() : 0.0f, obj.has("pitchErrorNorm") ? obj.get("pitchErrorNorm").getAsFloat() : 0.0f, obj.has("preAttackNorm") ? obj.get("preAttackNorm").getAsFloat() : 0.0f);
        }

        private static float pointMove(PathNode point) {
            return PathNode.pointMove(point.getYawDeltaDeg(), point.getPitchDeltaDeg());
        }

        private static float pointMove(float yawDelta, float pitchDelta) {
            return (float)Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
        }
    }
}

