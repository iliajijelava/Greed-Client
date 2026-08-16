package fun.ogi.util.rotation;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.player.EventMoveInput;
import fun.ogi.events.render.EventUpdate;
import net.minecraft.util.math.MathHelper;

import static fun.ogi.util.MinecraftUtil.mc;

public class RotationComponent {
    private static final RotationComponent INSTANCE = new RotationComponent();

    private RotationTask currentTask = RotationTask.IDLE;
    private float currentYawSpeed;
    private float currentPitchSpeed;
    private float currentYawReturnSpeed;
    private float currentPitchReturnSpeed;
    private int currentPriority;
    private int currentTimeout;
    private int idleTicks;
    private Rotation targetRotation;
    private Rotation currentRotation;

    public static RotationComponent getInstance() {
        return INSTANCE;
    }

    public Rotation getCurrentRotation() {
        return currentRotation != null ? currentRotation : new Rotation(mc.player);
    }

    public static double direction(float rotationYaw, final float moveForward, final float moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;
        float forward = 1F;
        if (moveForward < 0F) forward = -0.5F;
        if (moveForward > 0F) forward = 0.5F;
        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;
        return Math.toRadians(rotationYaw);
    }

    public static void fixMovement(final EventMoveInput event, final float yaw) {
        final float forward = event.getForward();
        final float strafe = event.getStrafe();

        if (forward == 0 && strafe == 0) {
            return;
        }

        final double targetAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, forward, strafe)));

        float bestForward = 0, bestStrafe = 0;
        float smallestDifference = Float.MAX_VALUE;

        for (float testForward = -1F; testForward <= 1F; testForward++) {
            for (float testStrafe = -1F; testStrafe <= 1F; testStrafe++) {
                if (testForward == 0 && testStrafe == 0) continue;

                final double testAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, testForward, testStrafe)));
                final float difference = Math.abs(MathHelper.wrapDegrees((float) (targetAngle - testAngle)));

                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    bestForward = testForward;
                    bestStrafe = testStrafe;
                }
            }
        }

        event.setForward(bestForward);
        event.setStrafe(bestStrafe);
    }

    @Subscribe
    private void onEvent(EventMoveInput event) {
        if (isRotating()) {
            fixMovement(event, MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw()));
        }
    }

    private void resetRotation() {
        Rotation returnTarget = new Rotation(FreeLookComponent.getFreeYaw(), FreeLookComponent.getFreePitch());
        if (updateRotation(returnTarget, currentYawReturnSpeed, currentPitchReturnSpeed)) {
            stopRotation();
        }
    }

    @Subscribe
    private void onEventUpdate(EventUpdate event) {
        if (currentTask.equals(RotationTask.AIM) && idleTicks > currentTimeout) {
            currentTask = RotationTask.RESET;
        }

        if (currentTask.equals(RotationTask.RESET)) {
            resetRotation();
        }
        idleTicks++;
    }

    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        INSTANCE.startUpdate(target, yawSpeed, pitchSpeed, yawReturnSpeed, pitchReturnSpeed, timeout, priority, clientRotation);
    }

    private void startUpdate(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        if (currentPriority > priority) {
            return;
        }

        if (currentTask.equals(RotationTask.IDLE) && !clientRotation) {
            FreeLookComponent.setActive(true);
        }

        currentYawSpeed = yawSpeed;
        currentPitchSpeed = pitchSpeed;
        currentYawReturnSpeed = yawReturnSpeed;
        currentPitchReturnSpeed = pitchReturnSpeed;
        currentTimeout = timeout;
        currentPriority = priority;
        currentTask = RotationTask.AIM;
        targetRotation = target;

        updateRotation(target, yawSpeed, pitchSpeed);
    }

    public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
        update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    public static void update(Rotation targetRotation, float yawSpeed, float pitchSpeed, float returnSpeed, int timeout, int priority) {
        update(targetRotation, yawSpeed, pitchSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    private boolean updateRotation(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
        if (mc.player == null) return false;
        final GCDFixer gcdFixer = new GCDFixer();
        Rotation playerRotation = new Rotation(mc.player);
        float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - playerRotation.getYaw());
        float pitchDelta = targetRotation.getPitch() - playerRotation.getPitch();

        float clampedYaw = Math.min(Math.abs(yawDelta), yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);

        float yaw = mc.player.getYaw();
        yaw += gcdFixer.getFixRotate(MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw));
        mc.player.setYaw(yaw);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + gcdFixer.getFixRotate(MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch)), -90F, 90F));

        currentRotation = new Rotation(mc.player);
        idleTicks = 0;
        return currentRotation.getDelta(targetRotation) < 1F;
    }

    public void stopRotation() {
        currentTask = RotationTask.IDLE;
        currentPriority = 0;
        FreeLookComponent.setActive(false);
    }

    public boolean isRotating() {
        return !currentTask.equals(RotationTask.IDLE);
    }

    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }
}

