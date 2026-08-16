package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventFrame;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.NumberSetting;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@ModuleInformation(moduleName = "Legit Aim", moduleDesc = "Smooth aim assist (ryzen poshel naxui)", moduleCategory = ModuleCategory.COMBAT)
public class AimAssist extends Module {

    private final ListSetting targets = new ListSetting("Targets", this, "Players", "Mobs", "Animals");
    private final NumberSetting range = new NumberSetting("Range", this, 5.0, 1.0, 10.0, 0.5);
    private final NumberSetting fov = new NumberSetting("FOV", this, 90.0, 30.0, 180.0, 5.0);
    
    private final NumberSetting fovHold = new NumberSetting("FOV Hold", this, 15.0, 0.0, 45.0, 5.0);
    private final NumberSetting speed = new NumberSetting("Speed", this, 5.0, 1.0, 10.0, 0.5);
    private final NumberSetting strength = new NumberSetting("Strength", this, 5.0, 1.0, 10.0, 0.5);
    private final ModeSetting aimPosition = new ModeSetting("Aim Point", this, "Random","Random", "Head", "Body", "Legs");
    private final BooleanSetting throughWalls = new BooleanSetting("Through Walls", this, false);
    private final BooleanSetting verticalAim = new BooleanSetting("Vertical Aim", this, true);
    private final ModeSetting priority = new ModeSetting("Priority", this, "Distance", "Distance", "FOV", "Health");

    private LivingEntity currentTarget;
    private long lastTargetTime = 0;
    private final Random random = new Random();

    public AimAssist() {
        addSettings(targets, range, fov, fovHold, speed, strength,
                aimPosition, throughWalls, verticalAim, priority);
        targets.select("Players");
    }

    @Subscribe
    public void onFrame(EventFrame e) {
        if (mc.player == null || mc.world == null) return;

        float dt = e.getDeltaTime();
        if (dt <= 0) return;

        LivingEntity target = findTarget();

        if (target == null && currentTarget != null && System.currentTimeMillis() - lastTargetTime < 150) {
            if (isValidTarget(currentTarget) && mc.player.distanceTo(currentTarget) <= range.getFloatValue()) {
                target = currentTarget;
            }
        }

        if (target == null) {
            currentTarget = null;
            return;
        }

        currentTarget = target;
        lastTargetTime = System.currentTimeMillis();

        Vec3d aimPoint = getAimPoint(target);
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d delta = aimPoint.subtract(playerPos);
        double hDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float targetYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, hDist));

        float curYaw = mc.player.getYaw();
        float curPitch = mc.player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(targetYaw - curYaw);
        float pitchDiff = targetPitch - curPitch;

        
        
        float hardLimit = fov.getFloatValue() + fovHold.getFloatValue();
        if (Math.abs(yawDiff) > hardLimit) return;

        float fovDamping = 1.0f;
        if (Math.abs(yawDiff) > fov.getFloatValue() && fovHold.getFloatValue() > 0) {
            float over = Math.abs(yawDiff) - fov.getFloatValue();
            fovDamping = 1.0f - MathHelper.clamp(over / fovHold.getFloatValue(), 0f, 1f);
        }

        float str = strength.getFloatValue() / 10.0f;  
        float spd = speed.getFloatValue() / 10.0f;      

        
        
        
        float rate = 4.0f + str * 10.0f;           
        float lerp = 1.0f - (float) Math.exp(-rate * spd * dt);
        lerp = MathHelper.clamp(lerp, 0f, 1f) * fovDamping;

        float newYaw = curYaw + yawDiff * lerp;
        float newPitch = curPitch + pitchDiff * lerp;

        if (!verticalAim.getValue()) {
            newPitch = curPitch;
        }

        
        
        float noiseScale = (float) Math.sqrt(dt * 20.0);
        newYaw += (random.nextFloat() - 0.5f) * 0.03f * noiseScale;
        newPitch += (random.nextFloat() - 0.5f) * 0.02f * noiseScale;

        
        

        mc.player.setYaw(newYaw);
        mc.player.setPitch(MathHelper.clamp(newPitch, -89.0f, 89.0f));

    }

    private LivingEntity findTarget() {
        List<LivingEntity> list = new ArrayList<>();
        Box box = mc.player.getBoundingBox().expand(range.getFloatValue());
        Vec3d look = mc.player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = mc.player.getEyePos();

        for (LivingEntity e : mc.world.getEntitiesByClass(LivingEntity.class, box, x -> true)) {
            if (!isValidTarget(e)) continue;
            if (mc.player.distanceTo(e) > range.getFloatValue()) continue;
            if (!throughWalls.getValue() && !mc.player.canSee(e)) continue;

            Vec3d to = e.getEyePos().subtract(eyePos);
            double angle = angleBetween(look, to);
            if (angle > fov.getFloatValue()) continue;

            list.add(e);
        }

        if (list.isEmpty()) return null;

        switch (priority.getValue()) {
            case "Distance" -> list.sort(Comparator.comparingDouble(mc.player::distanceTo));
            case "FOV" -> list.sort(Comparator.comparingDouble(en -> {
                Vec3d to = en.getEyePos().subtract(eyePos);
                return angleBetween(look, to);
            }));
            case "Health" -> list.sort(Comparator.comparingDouble(LivingEntity::getHealth));
        }
        return list.get(0);
    }

    private double angleBetween(Vec3d normalizedLook, Vec3d to) {
        return Math.toDegrees(Math.acos(MathHelper.clamp(normalizedLook.dotProduct(to.normalize()), -1.0, 1.0)));
    }

    private boolean isValidTarget(LivingEntity e) {
        if (e == mc.player || !e.isAlive() || e.getHealth() <= 0) return false;
        if (e instanceof PlayerEntity && targets.isSelected("Players")) return true;
        if (e instanceof HostileEntity && targets.isSelected("Mobs")) return true;
        if (e instanceof AnimalEntity && targets.isSelected("Animals")) return true;
        return false;
    }

    private Vec3d getAimPoint(LivingEntity target) {
        Box b = target.getBoundingBox();
        double height = b.maxY - b.minY;
        double cx = (b.minX + b.maxX) / 2.0;
        double cz = (b.minZ + b.maxZ) / 2.0;

        return switch (aimPosition.getValue()) {
            case "Head" -> new Vec3d(cx, b.maxY - height * 0.1, cz);
            case "Legs" -> new Vec3d(cx, b.minY + height * 0.2, cz);
            case "Random" -> new Vec3d(cx, b.minY + height * random.nextDouble(), cz);
            default -> new Vec3d(cx, b.minY + height * 0.5, cz); 
        };
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentTarget = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
    }
}