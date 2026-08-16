package fun.ogi.module.impl.list.movement;

import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.chatutil.ChatUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;


@ModuleInformation(moduleName = "Elytra boost", moduleDesc = "Increases speed on elytras",moduleCategory = ModuleCategory.MOVEMENT)
public class ElytraBoost extends Module {

    private static final String[] RANGE_LABELS = {
            "0 - 5", "5 - 10", "10 - 15", "15 - 20", "20 - 25",
            "25 - 30", "30 - 35", "35 - 40", "40 - 45"
    };

    private static final int[] AI_YAW_VECTORS = {-45, 45, 135, -135};
    private static final int[] AI_PITCH_VECTORS = {-45, 45};

    public static ElytraBoost INSTANCE = new ElytraBoost();

    private final ModeSetting mode = new ModeSetting(
            "Mode",this,
            "ReallyWorld",
            "ReallyWorld",
            "Bravo",
            "Custom"
    );


    private final BooleanSetting autoDecrease = new BooleanSetting("Auto decrease on flag",this ,false)
            .visible(() -> mode.getValueAsString().equals("Custom"));

    private final SliderSetting[] xzSpeeds = new SliderSetting[9];
    private final SliderSetting[] yUpSpeeds = new SliderSetting[9];
    private final SliderSetting[] yDownSpeeds = new SliderSetting[9];

    private String lastAngleRangeXZ = "";
    private String lastAngleRangeY = "";
    private boolean lastYUp = true;

    private double lastX = 0;
    private double lastY = 0;
    private double lastZ = 0;

    public ElytraBoost() {

        String[] xzNames = {
                "XZ 0-5", "XZ 5-10", "XZ 10-15", "XZ 15-20", "XZ 20-25",
                "XZ 25-30", "XZ 30-35", "XZ 35-40", "XZ 40-45"
        };
        String[] yUpNames = {
                "Y Вверх 0-5", "Y Вверх 5-10", "Y Вверх 10-15", "Y Вверх 15-20", "Y Вверх 20-25",
                "Y Вверх 25-30", "Y Вверх 30-35", "Y Вверх 35-40", "Y Вверх 40-45"
        };
        String[] yDownNames = {
                "Y Вниз 0-5", "Y Вниз 5-10", "Y Вниз 10-15", "Y Вниз 15-20", "Y Вниз 20-25",
                "Y Вниз 25-30", "Y Вниз 30-35", "Y Вниз 35-40", "Y Вниз 40-45"
        };

        for (int i = 0; i < 9; i++) {
            xzSpeeds[i] = new SliderSetting(xzNames[i], this,.6f, 1.5f, 2.5f, 0.01f).visible(() -> mode.getValueAsString().equals("Custom"));
            yUpSpeeds[i] = new SliderSetting(yUpNames[i], this,1.6f, 1.5f, 2.5f, 0.01f).visible(() -> mode.getValueAsString().equals("Custom"));
            yDownSpeeds[i] = new SliderSetting(yDownNames[i],this, 1.6f, 1.5f, 2.5f, 0.01f).visible(() -> mode.getValueAsString().equals("Custom"));
        }

        addSettings(mode, autoDecrease);
        addSettings(xzSpeeds);
        addSettings(yUpSpeeds);
        addSettings(yDownSpeeds);

        applyReallyWorldPreset();
    }

    public Vec2f computeBoost(float yaw, float pitch) {
        if (mode.is("Кастомный")) {
            float xz = getCustomSpeedXZ(yaw);
            float y = getCustomSpeedY(pitch);
            return new Vec2f(xz, y);
        } else if (mode.is("Bravo")) {
            float xz = getBravoSpeedXZ(pitch, yaw);
            float y = getBravoSpeedY(pitch);
            return new Vec2f(xz, y);
        } else {
            float speed = getAiBoost(pitch, yaw, false, true);
            return new Vec2f(speed, speed);
        }
    }

    public void handleFlag(double px, double py, double pz) {
        if (!autoDecrease.getValue() || !mode.is("Кастомный")) return;

        double deltaX = Math.abs(px - lastX);
        double deltaY = Math.abs(py - lastY);
        double deltaZ = Math.abs(pz - lastZ);
        double deltaXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (deltaXZ < 0.01 && deltaY < 0.01) {
            lastX = px; lastY = py; lastZ = pz;
            return;
        }

        if (deltaXZ > deltaY) {
            SliderSetting target = getSliderForRangeXZ(lastAngleRangeXZ);
            if (target != null) {
                float newVal = Math.max(1.5f, target.getFloatValue() - 0.01f);
                target.setValue(newVal);
                ChatUtil.sendMSG("Флагнут XZ: " + lastAngleRangeXZ + "° (авто-уменьшено на 0.01)");
            }
        } else {
            SliderSetting target = getSliderForRangeY(lastAngleRangeY, lastYUp);
            if (target != null) {
                float newVal = Math.max(1.5f, target.getFloatValue() - 0.01f);
                target.setValue(newVal);
                ChatUtil.sendMSG("Флагнут Y: " + lastAngleRangeY + "° (авто-уменьшено на 0.01)");
            }
        }

        lastX = px; lastY = py; lastZ = pz;
    }

    public void saveLastPos(double x, double y, double z) {
        lastX = x; lastY = y; lastZ = z;
    }

    private float getCustomSpeedXZ(float yaw) {
        float converted = convertAngleToRange(yaw);
        if (converted <= 5f) { lastAngleRangeXZ = "0-5"; return xzSpeeds[0].getFloatValue(); }
        if (converted <= 10f) { lastAngleRangeXZ = "5-10"; return xzSpeeds[1].getFloatValue(); }
        if (converted <= 15f) { lastAngleRangeXZ = "10-15"; return xzSpeeds[2].getFloatValue(); }
        if (converted <= 20f) { lastAngleRangeXZ = "15-20"; return xzSpeeds[3].getFloatValue(); }
        if (converted <= 25f) { lastAngleRangeXZ = "20-25"; return xzSpeeds[4].getFloatValue(); }
        if (converted <= 30f) { lastAngleRangeXZ = "25-30"; return xzSpeeds[5].getFloatValue(); }
        if (converted <= 35f) { lastAngleRangeXZ = "30-35"; return xzSpeeds[6].getFloatValue(); }
        if (converted <= 40f) { lastAngleRangeXZ = "35-40"; return xzSpeeds[7].getFloatValue(); }
        lastAngleRangeXZ = "40-45"; return xzSpeeds[8].getFloatValue();
    }

    private float getCustomSpeedY(float pitch) {
        float converted = convertAngleToRange(pitch);
        boolean up = pitch < 0;
        lastYUp = up;
        SliderSetting[] arr = up ? yUpSpeeds : yDownSpeeds;
        if (converted <= 5f) { lastAngleRangeY = "0-5"; return arr[0].getFloatValue(); }
        if (converted <= 10f) { lastAngleRangeY = "5-10"; return arr[1].getFloatValue(); }
        if (converted <= 15f) { lastAngleRangeY = "10-15"; return arr[2].getFloatValue(); }
        if (converted <= 20f) { lastAngleRangeY = "15-20"; return arr[3].getFloatValue(); }
        if (converted <= 25f) { lastAngleRangeY = "20-25"; return arr[4].getFloatValue(); }
        if (converted <= 30f) { lastAngleRangeY = "25-30"; return arr[5].getFloatValue(); }
        if (converted <= 35f) { lastAngleRangeY = "30-35"; return arr[6].getFloatValue(); }
        if (converted <= 40f) { lastAngleRangeY = "35-40"; return arr[7].getFloatValue(); }
        lastAngleRangeY = "40-45"; return arr[8].getFloatValue();
    }

    private float getAiBoost(float pitch, float yaw, boolean isBravo, boolean applyRwCap) {
        if (Math.abs(pitch) > 55.0f) return 1.55f;
        float boost = adjustBoostForYaw(yaw, applyRwCap);
        boost = adjustBoostForPitch(pitch, boost);
        boost = Math.max(isBravo ? 1.65f : 1.6f, boost);
        return Math.min(boost, isBravo ? 1.9f : 2.2f);
    }

    private float adjustBoostForYaw(float yaw, boolean applyRwCap) {
        int idx = findClosestVector(yaw, AI_YAW_VECTORS);
        if (idx == -1) return 1.6f;
        float dist = Math.abs(MathHelper.wrapDegrees(yaw) - AI_YAW_VECTORS[idx]);
        float maxBoost = 2.2f, minBoostVal = 1.6f, maxDistance = 12f, smartBoost = 0.0f;
        if (dist <= maxDistance) {
            float ratio = dist / maxDistance;
            smartBoost = maxBoost - (maxBoost - minBoostVal) * ratio;
        }
        float variableSpeed = getVariableSpeed(dist);
        float finalSpeed = Math.max(smartBoost, variableSpeed);
        return applyRwCap ? Math.min(finalSpeed, 1.8f) : finalSpeed;
    }

    private float adjustBoostForPitch(float pitch, float boost) {
        int idx = findClosestVector(pitch, AI_PITCH_VECTORS);
        if (idx == -1) return boost;
        float dist = Math.abs(Math.abs(pitch) - Math.abs(AI_PITCH_VECTORS[idx]));
        if (dist < 30.0f) boost += 0.4f * (1.0f - dist / 30.0f);
        return boost;
    }

    private static float getVariableSpeed(float dist) {
        float[] thresholds = {4, 8, 11, 15, 21, 28};
        float[] speeds = {2.2f, 2.1f, 2.0f, 1.9f, 1.8f, 1.7f, 1.6f};
        int level = 0;
        while (level < thresholds.length && dist >= thresholds[level]) level++;
        return speeds[level];
    }

    private static int findClosestVector(float angle, int[] vectors) {
        int minIdx = -1;
        float minDist = Float.MAX_VALUE;
        for (int i = 0; i < vectors.length; i++) {
            float d = Math.abs(MathHelper.wrapDegrees(angle) - vectors[i]);
            if (d < minDist) { minDist = d; minIdx = i; }
        }
        return minIdx;
    }

    private float getBravoSpeedXZ(float pitch, float yaw) {
        float absPitch = Math.abs(pitch);
        float absYaw = Math.abs(MathHelper.wrapDegrees(yaw) % 90);
        float speed;
        if (absPitch >= 38 && absPitch <= 52) speed = 2.0f;
        else if (absPitch >= 32 && absPitch <= 58) speed = 1.96f;
        else if (absPitch >= 28 && absPitch <= 62) speed = 1.95f;
        else if ((absYaw >= 29 && absYaw <= 61) || (absPitch >= 29 && absPitch <= 61)) speed = 1.963f;
        else if ((absYaw >= 28 && absYaw <= 60) || (absPitch >= 28 && absPitch <= 60)) speed = 1.954f;
        else if ((absYaw >= 26 && absYaw <= 64) || (absPitch >= 26 && absPitch <= 64)) speed = 1.874f;
        else if ((absYaw >= 24 && absYaw <= 66) || (absPitch >= 24 && absPitch <= 66)) speed = 1.72f;
        else if ((absYaw >= 15 && absYaw <= 75) || (absPitch >= 15 && absPitch <= 75)) speed = 1.72f;
        else if ((absYaw >= 13 && absYaw <= 77) || (absPitch >= 13 && absPitch <= 77)) speed = 1.72f;
        else if ((absYaw >= 12 && absYaw <= 78) || (absPitch >= 12 && absPitch <= 78)) speed = 1.72f;
        else if ((absYaw >= 8 && absYaw <= 82) || (absPitch >= 11 && absPitch <= 79)) speed = 1.72f;
        else if ((absYaw >= 5 && absYaw <= 85) || (absPitch >= 8 && absPitch <= 82)) speed = 1.67f;
        else speed = 1.71f;
        return pitch > 15 ? speed - 0.068f : speed;
    }

    private float getBravoSpeedY(float pitch) {
        float absPitch = Math.abs(pitch);
        if (absPitch >= 37 && absPitch <= 38) return 2.03f;
        if (absPitch >= 25 && absPitch <= 30) return 2.0f;
        if (absPitch >= 35 && absPitch <= 45) return 1.99f;
        if (absPitch >= 40 && absPitch <= 50) return 1.97f;
        if (absPitch >= 50 && absPitch <= 60) return 1.96f;
        if (absPitch >= 51 && absPitch <= 61) return 1.85f;
        if (absPitch >= 52 && absPitch <= 65) return 1.8f;
        return 1.59f;
    }

    private float convertAngleToRange(float angle) {
        float abs = Math.abs(angle);
        if (abs > 90.0f) abs = 180.0f - abs;
        if (abs > 45.0f) abs = 90.0f - abs;
        return abs;
    }

    private SliderSetting getSliderForRangeXZ(String range) {
        switch (range) {
            case "0-5": return xzSpeeds[0];
            case "5-10": return xzSpeeds[1];
            case "10-15": return xzSpeeds[2];
            case "15-20": return xzSpeeds[3];
            case "20-25": return xzSpeeds[4];
            case "25-30": return xzSpeeds[5];
            case "30-35": return xzSpeeds[6];
            case "35-40": return xzSpeeds[7];
            case "40-45": return xzSpeeds[8];
            default: return null;
        }
    }

    private SliderSetting getSliderForRangeY(String range, boolean up) {
        SliderSetting[] arr = up ? yUpSpeeds : yDownSpeeds;
        switch (range) {
            case "0-5": return arr[0];
            case "5-10": return arr[1];
            case "10-15": return arr[2];
            case "15-20": return arr[3];
            case "20-25": return arr[4];
            case "25-30": return arr[5];
            case "30-35": return arr[6];
            case "35-40": return arr[7];
            case "40-45": return arr[8];
            default: return null;
        }
    }

    private void applyReallyWorldPreset() {
        float[] xz = {1.61f, 1.61f, 1.64f, 1.68f, 1.74f, 1.8f, 1.8f, 1.8f, 1.79f};
        float[] yUp = {1.61f, 1.58f, 1.6f, 1.61f, 1.68f, 1.7f, 1.77f, 1.66f, 1.94f};
        float[] yDown = {1.87f, 2.06f, 2.09f, 2.12f, 2.2f, 2.2f, 2.23f, 2.06f, 2.08f};
        for (int i = 0; i < 9; i++) { xzSpeeds[i].setValue(xz[i]); yUpSpeeds[i].setValue(yUp[i]); yDownSpeeds[i].setValue(yDown[i]); }
    }

    private void applyGrimPreset() {
        float[] xz = {1.61f, 1.63f, 1.66f, 1.69f, 1.77f, 1.83f, 1.93f, 2.08f, 2.24f};
        float[] yUp = {1.61f, 1.63f, 1.66f, 1.69f, 1.77f, 1.83f, 1.93f, 2.03f, 2.24f};
        float[] yDown = {1.61f, 1.63f, 1.66f, 1.68f, 1.77f, 1.83f, 1.93f, 2.08f, 2.24f};
        for (int i = 0; i < 9; i++) { xzSpeeds[i].setValue(xz[i]); yUpSpeeds[i].setValue(yUp[i]); yDownSpeeds[i].setValue(yDown[i]); }
    }

    private void applyLonyGriefPreset() {
        float[] xz = {1.61f, 1.63f, 1.65f, 1.7f, 1.73f, 1.83f, 1.94f, 2.07f, 2.18f};
        float[] yUp = {1.63f, 1.61f, 1.61f, 1.63f, 1.66f, 1.7f, 1.78f, 2.03f, 2.03f};
        float[] yDown = {1.63f, 1.63f, 1.66f, 1.68f, 1.77f, 1.83f, 1.93f, 2.08f, 2.24f};
        for (int i = 0; i < 9; i++) { xzSpeeds[i].setValue(xz[i]); yUpSpeeds[i].setValue(yUp[i]); yDownSpeeds[i].setValue(yDown[i]); }
    }
}

