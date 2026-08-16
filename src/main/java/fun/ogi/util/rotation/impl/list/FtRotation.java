package fun.ogi.util.rotation.impl.list;

import fun.ogi.module.impl.list.combat.AttackAura;
import fun.ogi.util.combat.FantimeUtil;
import fun.ogi.util.render.math.MathUtil;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import fun.ogi.util.rotation.impl.RotationSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

import static fun.ogi.util.MinecraftUtil.mc;

public abstract class FtRotation extends RotationSystem {

    private final float[] pitchHistory = new float[30];
    private int ticks;
    private float phase;
    private float snapTimer;
    private float cooldown;
    private boolean initialized;

    protected FtRotation(AttackAura aura) {
        super(aura);
    }

    protected abstract boolean useCurrentPitch();

    public void resetState() {
        initialized = false;
        ticks = 0;
        phase = 0;
        snapTimer = 2.0f;
        cooldown = 1.0f;
        Arrays.fill(pitchHistory, 0.0f);
    }

    @Override
    public void update(LivingEntity target) {
        if (mc.player == null || mc.world == null || target == null) return;
        if (!initialized) {
            resetState();
            initialized = true;
        }

        Vec3d eye = mc.player.getEyePos();
        double aimReach = aura.distance.getFloatValue();
        boolean allowThroughWalls = aura.throughWalls.getValue();

        Vec3d aimDelta = FantimeUtil.getAimPoint(eye, target, aimReach, allowThroughWalls);
        float yawToTarget;
        float pitchToTarget;
        if (aimDelta == Vec3d.ZERO) {
            yawToTarget = mc.player.getYaw();
            pitchToTarget = mc.player.getPitch();
        } else {
            yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(aimDelta.z, aimDelta.x)) - 90.0d);
            pitchToTarget = (float) (-Math.toDegrees(Math.atan2(aimDelta.y, Math.hypot(aimDelta.x, aimDelta.z))));
        }

        System.arraycopy(pitchHistory, 0, pitchHistory, 1, 29);
        pitchHistory[0] = pitchToTarget;

        float t = mc.player.age + mc.getRenderTickCounter().getTickDelta(false);
        float smoothW = (float) ((Math.sin(t * 0.4000000008323731d) * 3.0d) + (Math.sin((t * 0.9500002390239708d) + 1.4000004888461306d) * 2.0d));
        float smoothH = (float) ((Math.cos((t * 0.5d) + 0.7000001555309916d) * 0.5d) + (Math.cos((t * 0.7800000620494261d) + 3.10000031689524d) * 1.5d));
        float finalPitch = FantimeUtil.lerpAngle(mc.player.getPitch(), pitchHistory[MathHelper.clamp(10 - ticks, 0, 29)] + (smoothH * 1.5f), MathUtil.random(0.1f, 0.5f));
        float finalYaw = FantimeUtil.lerpAngle(mc.player.getYaw(), yawToTarget + smoothW, MathUtil.random(0.1f, 0.4f));

        if (cooldown >= 0.0f) {
            if (!FantimeUtil.canSee(mc.player.getYaw(), mc.player.getPitch(), aimReach, target, true) && snapTimer <= 0.0f) {
                finalYaw = yawToTarget;
            }
            if (!FantimeUtil.canSee(yawToTarget, finalPitch, aimReach, target, true) && snapTimer <= 0.0f) {
                finalPitch = pitchToTarget;
            }
            if (!FantimeUtil.canSee(mc.player.getYaw() + smoothW, mc.player.getYaw() + smoothH, aimReach, target, true)
                    && FantimeUtil.canSee(mc.player.getYaw(), mc.player.getPitch(), aimReach, target, true)) {
                smoothW = MathHelper.clamp(smoothW, -0.05f, 0.05f);
                smoothH = MathHelper.clamp(smoothH, -0.05f, 0.05f);
            }
        }
        if (ticks <= 4 && phase % 2.0f == 0.0f) {
            finalYaw = mc.player.getYaw();
        }

        float outPitch = useCurrentPitch() ? mc.player.getPitch() : finalPitch;
        RotationComponent.update(new Rotation(finalYaw + smoothW, outPitch + smoothH), 220f, 220f, 220f, 220f, 9, 1, aura.clientLook.getValue());

        phase += 1;
        snapTimer -= 1;
        cooldown -= 1;
        ticks++;
        if (ticks >= 10) {
            ticks = 0;
            cooldown = 1.0f;
        }
    }
}

