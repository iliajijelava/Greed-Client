package fun.ogi.util.rotation.impl.list;

import fun.ogi.module.impl.list.combat.AttackAura;
import fun.ogi.util.combat.BestPoint;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import fun.ogi.util.rotation.RotationUtil;
import fun.ogi.util.rotation.impl.RotationSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class ReallyWorldRotation extends RotationSystem {

    public ReallyWorldRotation(AttackAura aura) {
        super(aura);
    }

    @Override
    public void update(LivingEntity target) {
        if (target == null) return;

        Vec3d targetPoint = aura.smartAim.getValue() ? BestPoint.getPoint(target) : target.getEyePos();

        Vec2f angle = new RotationUtil().calculate(targetPoint);
        float yaw = MathHelper.wrapDegrees(angle.x);
        float pitch = MathHelper.clamp(angle.y, -90f, 90f);
        RotationComponent.update(new Rotation(yaw, pitch), 360, 360, 360, 360, 0, 1, aura.clientLook.getValue());
    }
}

