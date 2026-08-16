package fun.ogi.util.rotation;


import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import static fun.ogi.util.MinecraftUtil.mc;


public class Rotation  {
    private float yaw, pitch;
    private RotationUtil rotationUtil = new RotationUtil();
    public static final Rotation ZERO = new Rotation(0.0F, 0.0F);
    public float getYaw(){
        return yaw;
    }

    public float getPitch(){
        return pitch;
    }
    public Rotation(Entity entity) {
        yaw = entity.getYaw();
        pitch = entity.getPitch();
    }

    public Rotation(Vec2f vec) {
        yaw = vec.x;
        pitch = vec.y;
    }

    public Rotation(Vec3d vec) {
        yaw = rotationUtil.calculate(vec).x;
        pitch = rotationUtil.calculate(vec).y;
    }
    public Rotation(float yaw, float pitch){
        this.yaw = yaw;
        this.pitch = pitch;
    }
    public float getDelta(Rotation target) {
        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - this.yaw);
        float pitchDelta = target.getPitch() - this.pitch;
        return (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
    }

    public double getDeltaDouble(Rotation target) {
        double yawDelta = MathHelper.wrapDegrees(target.getYaw() - yaw);
        double pitchDelta = MathHelper.wrapDegrees(target.getPitch() - pitch);
        return Math.hypot(yawDelta, pitchDelta);
    }

    public static Vector2f camera() {
        return new Vector2f(cameraYaw(), cameraPitch());
    }

    public static float cameraYaw() {
        return MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() + (mc.gameRenderer.getCamera().isThirdPerson() ? 180 : 0));
    }

    public static float cameraPitch() {
        return (mc.gameRenderer.getCamera().isThirdPerson() ? -1 : 1) * mc.gameRenderer.getCamera().getPitch();
    }

    public static Rotation from(PlayerEntity player, Entity target) {
        Vec3d playerPos = player.getCameraPosVec(0);
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);

        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distanceXZ));

        return new Rotation(yaw, pitch);
    }
    public static Rotation fromVec3d(Vec3d vec) {
        if (mc.player == null) return ZERO;
        return fromVec3d(mc.player.getEyePos(), vec);
    }
    public static Rotation fromVec3d(Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);
        double hDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float pitch = (float) Math.toDegrees(Math.atan2(diff.y, hDist));
        return new Rotation(yaw, pitch);
    }
    public final Vec3d toVector() {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }
}