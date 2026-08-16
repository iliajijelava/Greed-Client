package fun.ogi.rotation.ainew;

import fun.ogi.util.rotation.Rotation;
import net.minecraft.util.math.MathHelper;

public final class AINewRotationHelper {
    private AINewRotationHelper() {}

    public static Rotation normalize(Rotation rot, Rotation base) {
        float deltaYaw = MathHelper.wrapDegrees(rot.getYaw() - base.getYaw());
        return new Rotation(base.getYaw() + deltaYaw, MathHelper.clamp(rot.getPitch(), -90.0f, 90.0f));
    }
}

