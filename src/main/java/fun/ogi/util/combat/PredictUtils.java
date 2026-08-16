package fun.ogi.util.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static fun.ogi.util.MinecraftUtil.mc;

public class PredictUtils {

    private static final Map<UUID, PositionData> positionCache = new ConcurrentHashMap<>();

    public static class PositionData {
        private double serverX, serverY, serverZ;
        private double prevServerX, prevServerY, prevServerZ;
        private double backUpX, backUpY, backUpZ;
        private double lastSpeed, prevSpeed;
        private long lastUpdate;

        public Vec3d getResolvedPos() {
            return new Vec3d(serverX, serverY, serverZ);
        }

        public Vec3d getResolvedForward() {
            return new Vec3d(
                    serverX - prevServerX,
                    serverY - prevServerY,
                    serverZ - prevServerZ
            );
        }

        public void update(double x, double y, double z) {
            backUpX = prevServerX;
            backUpY = prevServerY;
            backUpZ = prevServerZ;

            prevServerX = serverX;
            prevServerY = serverY;
            prevServerZ = serverZ;
            serverX = x;
            serverY = y;
            serverZ = z;

            prevSpeed = lastSpeed;
            lastSpeed = getResolvedForward().length() * 20.0;
            lastUpdate = System.currentTimeMillis();
        }

        public boolean isSpeedChanged() {
            return lastSpeed >= 20.0 || (lastSpeed != prevSpeed && lastSpeed == 0.0);
        }

        public double getLastSpeed() {
            return lastSpeed;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }
    }

    public static void updateEntity(LivingEntity entity) {
        PositionData data = positionCache.computeIfAbsent(entity.getUuid(), k -> new PositionData());
        data.update(entity.getX(), entity.getY(), entity.getZ());
    }

    public static PositionData getData(LivingEntity entity) {
        return positionCache.get(entity.getUuid());
    }

    public static Vec3d predict(LivingEntity entity, int ticks, float extraForward, boolean isMeFlying) {
        PositionData data = getData(entity);
        Vec3d pos = new Vec3d(entity.getX(), entity.getY() + entity.getStandingEyeHeight() / 2.0F, entity.getZ());

        if (data == null) {
            return predictElytraPhysics(entity, pos, ticks);
        }

        Vec3d forward = data.getResolvedForward();
        double speed = data.getLastSpeed();
        boolean isHighSpeed = data.isSpeedChanged();

        if (entity.isGliding()) {
            double horizontalSpeed = Math.hypot(forward.x, forward.z) * 20.0;
            double verticalSpeed = Math.abs(forward.y) * 20.0;

            if (horizontalSpeed <= 5.0 && verticalSpeed <= 5.0) {
                return pos;
            }

            boolean shouldPredict = isMeFlying && isHighSpeed;
            float predictMultiplier = shouldPredict ? ticks + 2.0f + extraForward : ticks;

            Vec3d linearPredict = pos.add(forward.multiply(predictMultiplier, predictMultiplier, predictMultiplier));
            Vec3d physicsPredict = predictElytraPhysics(entity, pos, ticks);

            double weight = MathHelper.clamp(speed / 50.0, 0.3, 0.9);
            return new Vec3d(
                    MathHelper.lerp(weight, physicsPredict.x, linearPredict.x),
                    MathHelper.lerp(weight, physicsPredict.y, linearPredict.y),
                    MathHelper.lerp(weight, physicsPredict.z, linearPredict.z)
            );
        }

        if (speed > 1.0) {
            return pos.add(forward.multiply(ticks, ticks, ticks));
        }

        return pos;
    }

    public static Vec3d predict(LivingEntity target, double ticksAhead) {
        double i2;
        if (target.isGliding()) {
            Vec3d vel = target.getVelocity();
            double speed = vel.length();
            if (speed < 0.01) {
                return target.getPos();
            }
            return target.getPos().add(vel.multiply(ticksAhead * 1.25));
        }
        if (Math.hypot(target.prevX - target.getX(), target.prevZ - target.getZ()) * 20.0 <= 1.0 && target.prevY - target.getY() <= 1.0) {
            return target.getPos();
        }
        Vec3d forward = Vec3d.fromPolar((float)(target.getPitch() + (target.getPitch() - target.prevPitch)), (float)(target.getYaw() + (target.getYaw() - target.prevYaw))).multiply(new Vec3d(target.getX() - target.prevX, target.getY() - target.prevY, target.getZ() - target.prevZ).length() * ticksAhead);
        Vec3d vec3d = target.getRotationVector(target.getPitch() + (target.getPitch() - target.prevPitch), target.getYaw() + (target.getYaw() - target.prevYaw));
        float f2 = target.getPitch() * ((float)Math.PI / 180);
        double d2 = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        double e2 = forward.horizontalLength();
        boolean bl = target.getVelocity().y <= 0.0;
        double g2 = bl && target.hasStatusEffect(StatusEffects.SLOW_FALLING) ? Math.min(target.getFinalGravity(), 0.01) : target.getFinalGravity();
        double h2 = MathHelper.square((double)Math.cos(f2));
        forward = forward.add(0.0, g2 * (-1.0 + h2 * 0.75), 0.0);
        if (forward.y < 0.0 && d2 > 0.0) {
            i2 = forward.y * -0.1 * h2;
            forward = forward.add(vec3d.x * i2 / d2, i2, vec3d.z * i2 / d2);
        }
        if (f2 < 0.0f && d2 > 0.0) {
            i2 = e2 * (double)(-MathHelper.sin((float)f2)) * 0.04;
            forward = forward.add(-vec3d.x * i2 / d2, i2 * (double)2.2f, -vec3d.z * i2 / d2);
        }
        if (d2 > 0.0) {
            forward = forward.add((vec3d.x / d2 * e2 - forward.x) * 0.1, 0.0, (vec3d.z / d2 * e2 - forward.z) * 0.1);
        }
        return target.getPos().add(forward);
    }

    public static Vec3d predict(LivingEntity entity, Vec3d pos, int ticks) {
        PositionData data = getData(entity);

        if (data != null && entity.isGliding()) {
            Vec3d forward = data.getResolvedForward();
            double horizontalSpeed = Math.hypot(forward.x, forward.z) * 20.0;
            double verticalSpeed = Math.abs(forward.y) * 20.0;

            if (horizontalSpeed <= 5.0 && verticalSpeed <= 5.0) {
                return pos;
            }

            return pos.add(forward.multiply(ticks, ticks, ticks));
        }

        return predictElytraPhysics(entity, pos, ticks);
    }

    public static Vec3d predictElytraPhysics(LivingEntity entity, Vec3d pos, int ticks) {
        Vec3d velocity = entity.getVelocity();

        if (!entity.isGliding()) {
            return pos.add(velocity.multiply(ticks, ticks, ticks));
        }

        double horizontalDelta = Math.hypot(entity.prevX - entity.getX(), entity.prevZ - entity.getZ()) * 20.0;
        double verticalDelta = Math.abs(entity.getY() - entity.prevY) * 20.0;

        if (horizontalDelta <= 5.0 && verticalDelta <= 5.0) {
            return pos;
        }

        for (int i = 0; i < ticks; i++) {
            Vec3d rotation = entity.getRotationVector();
            float pitchRad = (float) Math.toRadians(entity.getPitch());
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double velocityLength = velocity.length();
            float cos = MathHelper.cos(pitchRad);
            cos = (float) (cos * cos * Math.min(1.0D, rotation.length() / 0.4D));

            velocity = velocity.add(0.0D, -0.08D * (-1.0D + (double) cos * 0.75D), 0.0D);

            if (velocity.y < 0.0D && horizontalSpeed > 0.0D) {
                double lift = velocity.y * -0.1D * cos;
                velocity = velocity.add(rotation.x * lift / horizontalSpeed, lift, rotation.z * lift / horizontalSpeed);
            }

            if (pitchRad < 0.0F && horizontalSpeed > 0.0D) {
                double lift = velocityLength * (-MathHelper.sin(pitchRad)) * 0.04D;
                velocity = velocity.add(-rotation.x * lift / horizontalSpeed, lift * 3.2D, -rotation.z * lift / horizontalSpeed);
            }

            if (horizontalSpeed > 0.0D) {
                velocity = velocity.add(
                        (rotation.x / horizontalSpeed * velocityLength - velocity.x) * 0.1D,
                        0.0D,
                        (rotation.z / horizontalSpeed * velocityLength - velocity.z) * 0.1D
                );
            }

            velocity = velocity.multiply(0.99D, 0.98D, 0.99D);
            pos = pos.add(velocity);
        }

        return pos;
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        positionCache.entrySet().removeIf(e -> now - e.getValue().getLastUpdate() > 10000L);
    }

    public static void clear() {
        positionCache.clear();
    }
}

