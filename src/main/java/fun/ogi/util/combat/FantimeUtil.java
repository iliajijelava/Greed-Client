package fun.ogi.util.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static fun.ogi.util.MinecraftUtil.mc;

public class FantimeUtil {

    private static final double[] GRID = {0.0d, 0.125d, 0.25d, 0.375d, 0.5d, 0.625d, 0.75d, 0.875d, 1.0d};

    private FantimeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean canSee(float yaw, float pitch, double distance, Entity entity, boolean throughWalls) {
        if (mc.player == null || mc.world == null) return false;
        return canSee(mc.player.getEyePos(), yaw, pitch, distance, entity, throughWalls);
    }

    public static boolean canSee(Vec3d rayOrigin, float yaw, float pitch, double distance, Entity entity, boolean throughWalls) {
        if (mc.player == null || mc.world == null) return false;
        Vec3d dir = Vec3d.fromPolar(pitch, yaw).multiply(distance);
        Optional<Vec3d> opt = entity.getBoundingBox().contains(rayOrigin) ? Optional.of(rayOrigin) : entity.getBoundingBox().raycast(rayOrigin, rayOrigin.add(dir));
        if (opt.isEmpty()) return false;
        if (!throughWalls && mc.world.raycast(new RaycastContext(rayOrigin, opt.get(), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player)).getType() != HitResult.Type.MISS) {
            return false;
        }
        return true;
    }

    public static Vec3d getAimPoint(Vec3d eye, LivingEntity target, double reach, boolean throughWalls) {
        if (mc.player == null || mc.world == null || target == null) return Vec3d.ZERO;
        Box bb = target.getBoundingBox();
        double mx = (bb.minX + bb.maxX) * 0.5d;
        double mz = (bb.minZ + bb.maxZ) * 0.5d;
        Vec3d targetEye = target.getPos().add(0.0d, target.getStandingEyeHeight(), 0.0d);
        double distToTargetEye = eye.distanceTo(targetEye);
        double aimHeight = eye.y;
        double ay = MathHelper.lerp(MathHelper.clamp(distToTargetEye / 3.0d, 0.0d, 1.0d), bb.minY, MathHelper.clamp(aimHeight, bb.minY, bb.maxY));
        Vec3d ideal = new Vec3d(mx, ay, mz);

        List<Vec3d> pts = new ArrayList<>();
        pts.add(ideal);
        int last = GRID.length - 1;
        for (int a = 0; a < GRID.length; a++) {
            for (int b = 0; b < GRID.length; b++) {
                for (int c = 0; c < GRID.length; c++) {
                    if (a == 0 || a == last || b == 0 || b == last || c == 0 || c == last) {
                        pts.add(new Vec3d(MathHelper.lerp(GRID[a], bb.minX, bb.maxX), MathHelper.lerp(GRID[b], bb.minY, bb.maxY), MathHelper.lerp(GRID[c], bb.minZ, bb.maxZ)));
                    }
                }
            }
        }

        for (double pad : new double[]{0.0d, 0.20000001551382535d}) {
            List<Vec3d> visible = new ArrayList<>();
            for (Vec3d p : pts) {
                Vec3d d = p.subtract(eye);
                double len = d.length();
                double limit = reach + pad;
                if (len <= limit) {
                    float traceDist = (float) limit;
                    if (canSee(eye, yawFromDelta(d), pitchFromDelta(d), traceDist, target, false)) {
                        visible.add(p);
                    }
                }
            }
            if (!visible.isEmpty()) {
                Vec3d centroid = visible.stream().reduce(Vec3d.ZERO, Vec3d::add).multiply(1.0d / visible.size());
                return visible.stream().min(Comparator.comparingDouble(p -> p.squaredDistanceTo(centroid))).get().subtract(eye);
            }
            if (throughWalls) {
                List<Vec3d> through = new ArrayList<>();
                for (Vec3d p : pts) {
                    Vec3d d = p.subtract(eye);
                    double len = d.length();
                    double limit = reach + pad;
                    if (len <= limit) {
                        float traceDist = (float) limit;
                        if (canSee(eye, yawFromDelta(d), pitchFromDelta(d), traceDist, target, true)) {
                            through.add(p);
                        }
                    }
                }
                if (!through.isEmpty()) {
                    Vec3d centroid = through.stream().reduce(Vec3d.ZERO, Vec3d::add).multiply(1.0d / through.size());
                    return through.stream().min(Comparator.comparingDouble(p -> p.squaredDistanceTo(centroid))).get().subtract(eye);
                }
            }
        }
        return Vec3d.ZERO;
    }

    public static float lerpAngle(float start, float end, float amount) {
        float a = MathHelper.clamp(amount, 0.0f, 1.0f);
        float d = MathHelper.wrapDegrees(end - start);
        if (Math.abs(d) < 0.5f) return end;
        float stepped = MathHelper.wrapDegrees(start + (d * a));
        float patched = gcdFix(start, stepped);
        float remaining = MathHelper.wrapDegrees(end - patched);
        return Math.abs(remaining) < 0.5f ? end : patched;
    }

    private static float gcdFix(float lastYaw, float current) {
        double sens = (mc.options.getMouseSensitivity().getValue() * 0.6000000498956214d) + 0.19999998556632664d;
        double gcd = sens * sens * sens * 8.0d;
        return (float) (lastYaw + (Math.ceil((current - lastYaw) / gcd / 0.15000006556510925d) * gcd * 0.15000006556510925d));
    }

    private static float yawFromDelta(Vec3d d) {
        return (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0d);
    }

    private static float pitchFromDelta(Vec3d d) {
        return (float) (-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
    }
}

