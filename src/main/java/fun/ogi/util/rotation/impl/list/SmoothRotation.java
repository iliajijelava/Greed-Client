package fun.ogi.util.rotation.impl.list;

import fun.ogi.module.impl.list.combat.AttackAura;
import fun.ogi.util.combat.PredictUtils;
import fun.ogi.util.rotation.GCDFixer;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import fun.ogi.util.rotation.RotationUtil;
import fun.ogi.util.rotation.impl.RotationSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SmoothRotation extends RotationSystem {

    private float lastYaw;
    private float lastPitch;

    public SmoothRotation(AttackAura aura) {
        super(aura);
    }

    public void setLastRotation(Rotation rotation) {
        this.lastYaw = rotation.getYaw();
        this.lastPitch = rotation.getPitch();
    }

    public void resetState() {
        this.lastYaw = 0;
        this.lastPitch = 0;
    }

    @Override
    public void update(LivingEntity target) {
        if (target == null) return;

        Vec3d targetPoint;
        if (target.isGliding() && aura.predictate.getValue()) {
            Vec3d predicted = PredictUtils.predict(target, aura.predictValue.getValue());
            double boxHeight = target.getBoundingBox().maxY - target.getBoundingBox().minY;
            targetPoint = new Vec3d(predicted.x, predicted.y + boxHeight * 0.8, predicted.z);
        } else {
            targetPoint = target.getEyePos();
        }

        var angle = new Rotation(new RotationUtil().calculate(targetPoint));
        float targetYaw = angle.getYaw();
        float targetPitch = angle.getPitch();

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float deltaPitch = targetPitch - lastPitch;

        float speed = 1f;

        float newYaw = lastYaw + deltaYaw * speed;
        float newPitch = lastPitch + deltaPitch * speed;

        float gcd = new GCDFixer().getGCDValue();
        newYaw -= (newYaw - lastYaw) % gcd;
        newPitch -= (newPitch - lastPitch) % gcd;

        newPitch = MathHelper.clamp(newPitch, -90f, 90f);

        var smoothRot = new Rotation(newYaw, newPitch);
        RotationComponent.update(smoothRot, 360, 360, 360, 360, 0, 1, aura.clientLook.getValue());

        lastYaw = smoothRot.getYaw();
        lastPitch = smoothRot.getPitch();
    }
}

