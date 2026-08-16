package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.impl.list.combat.AttackAura;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.combat.BestPoint;
import net.minecraft.entity.LivingEntity;

@ModuleInformation(moduleName = "ElytraTarget", moduleDesc = "Targeting helpers for elytra fights", moduleCategory = ModuleCategory.MOVEMENT)
public class ElytraTarget extends Module {

    public final BooleanSetting predictate = new BooleanSetting("Predict", this, true);
    public final SliderSetting predictValue = new SliderSetting("Predict Value", this, 3.0, 1, 6, 0.1).visible(() -> predictate.getValue());

    public final BooleanSetting elytraSlowdown = new BooleanSetting("Slowdown", this, true);
    public final SliderSetting slowdownRadius = new SliderSetting("Slowdown Radius", this, 1.9, 1.0, 6.0, 0.1).visible(() -> elytraSlowdown.getValue());
    public final SliderSetting minSpeed = new SliderSetting("Min Speed", this, 0.2, 0.1, 1.0, 0.05).visible(() -> elytraSlowdown.getValue());

    public final BooleanSetting hitAfterOvertake = new BooleanSetting("Hit After Overtake", this, false);

    public ElytraTarget() {
        addSettings(predictate, predictValue, elytraSlowdown, slowdownRadius, minSpeed, hitAfterOvertake);
    }
    @Subscribe
    private void onUpdate(EventUpdate e){

    }
    public boolean canAttack(LivingEntity target) {
        if (target == null || mc.player == null) {
            return false;
        }

        AttackAura attackAura = Cheap.getInstance().getModuleStorage().get(AttackAura.class);
        float attackDist = attackAura != null ? attackAura.distance.getFloatValue() : 4.0f;
        double distNearest = mc.player.getEyePos().distanceTo(BestPoint.getNearestPoint(target));
        return distNearest <= (double) attackDist;
    }

    private static ElytraTarget getElytraTarget() {
        ElytraTarget target = Cheap.getInstance().getModuleStorage().get(ElytraTarget.class);
        return (target != null && target.isEnabled()) ? target : null;
    }

    public static class PredictateWrapper {
        public boolean getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null && target.predictate.getValue();
        }
    }

    public static class PredictValueWrapper {
        public double getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null ? target.predictValue.getValue() : 3.0;
        }

        public float getFloatValue() {
            return (float) getValue();
        }
    }

    public static class ElytraSlowdownWrapper {
        public boolean getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null && target.elytraSlowdown.getValue();
        }
    }

    public static class SlowdownRadiusWrapper {
        public double getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null ? target.slowdownRadius.getValue() : 3.0;
        }
    }

    public static class MinSpeedWrapper {
        public float getFloatValue() {
            ElytraTarget target = getElytraTarget();
            return target != null ? target.minSpeed.getFloatValue() : 0.3f;
        }
    }

    public static class HitAfterOvertakeWrapper {
        public boolean getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null && target.hitAfterOvertake.getValue();
        }
    }

    public static class ShowPredictPointWrapper {
        public boolean getValue() {
            ElytraTarget target = getElytraTarget();
            return target != null;
        }
    }
}

