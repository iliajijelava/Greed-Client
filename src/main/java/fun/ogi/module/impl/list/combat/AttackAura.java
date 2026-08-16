package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.events.network.EventUpdatePost;
import fun.ogi.events.player.EventMoveInput;
import fun.ogi.events.player.EventSprint;
import fun.ogi.events.render.EventHud;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.impl.list.movement.ElytraTarget;
import fun.ogi.module.impl.list.movement.FreeCam;
import fun.ogi.module.impl.list.movement.Sprint;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.StopWatch;
import fun.ogi.util.combat.BestPoint;
import fun.ogi.util.combat.IdealHitUtils;
import fun.ogi.util.combat.PredictUtils;
import fun.ogi.util.combat.RayTraceUtil;
import fun.ogi.util.neuro.rotation.AIRotationRecorder;
import fun.ogi.util.render.providers.ColorProvider;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import fun.ogi.util.rotation.RotationUtil;
import fun.ogi.util.rotation.impl.list.*;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

@ModuleInformation(moduleName = "AttackAura", moduleDesc = "Automatically attacks the nearest enemies", moduleCategory = ModuleCategory.COMBAT)
public class AttackAura extends Module {

    public final ModeSetting rotation = new ModeSetting("Rotation", this, "FtNew","ReallyWorld", "FtNew","Smooth","Legit","Neuro","FtTest","FtTestfov");
    public final ModeSetting rotationBehavior = new ModeSetting("Rotation Mode", this, "Smooth", "Smooth", "Snaps");
    private final ListSetting targets = new ListSetting("Targets", this, "Players", "Naked", "Monsters", "Animals");

    public final SliderSetting distance = new SliderSetting("Distance", this, 3, 2, 6, 0.1f);
    private final SliderSetting preRotation = new SliderSetting("Pre Distance", this, 1.5f, 0, 3, 0.1f);
    public final BooleanSetting throughWalls = new BooleanSetting("Through walls",this,false);
    public final BooleanSetting smartAim = new BooleanSetting("Smart Aim", this, true);
    public final BooleanSetting predictate = new BooleanSetting("Predict", this, true);
    public final SliderSetting predictValue = new SliderSetting("Predict Value", this, 3, 1, 5, 0.1f).visible(predictate::getValue);
    private final SliderSetting fovRadius = new SliderSetting("Fov Radius", this, 90.0, 10.0, 360.0, 1.0).visible(() -> rotationBehavior.getValueAsString().equals("Snaps") || rotation.getValueAsString().equals("Legit"));
    private final BooleanSetting renderFov = new BooleanSetting("Render Fov", this, true).visible(() -> rotationBehavior.getValueAsString().equals("Snaps") || rotation.getValueAsString().equals("Legit"));
    public final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed", this, 15, 1, 60, 1).visible(() -> rotation.getValueAsString().equals("Legit"));
    public final BooleanSetting elytraSlowdown = new BooleanSetting("Elytra Slowdown", this, true);
    public final ModeSetting slowdownMode = new ModeSetting("Sprint Reset", this, "Before Hit", "By Radius", "Before Hit").visible(elytraSlowdown::getValue);
    public final SliderSetting slowdownRadius = new SliderSetting("Slowdown Radius", this, 3.0f, 1.0f, 6.0f, 0.1f).visible(() -> elytraSlowdown.getValue() && slowdownMode.is("By Radius"));
    public final SliderSetting minSpeed = new SliderSetting("Min Speed", this, 0.3f, 0.1f, 0.9f, 0.05f).visible(() -> elytraSlowdown.getValue() && slowdownMode.is("By Radius"));
    public final SliderSetting preHitTicks = new SliderSetting("Ticks Before Hit", this, 3, 1, 10, 1).visible(() -> elytraSlowdown.getValue() && slowdownMode.is("Before Hit"));
    public final BooleanSetting hitAfterOvertake = new BooleanSetting("Hit Only After Overtake", this, true);

    public final ModeSetting moveFix = new ModeSetting("Movement Fix", this, "Focused", "None", "Focused", "Targeted");

    public final BooleanSetting onlySpace = new BooleanSetting("Only Space", this, false);
    public final BooleanSetting clientLook = new BooleanSetting("Client Look", this, false);
    public final BooleanSetting showPredictPoint = new BooleanSetting("Show Predict Point", this, true);
    public final BooleanSetting elytraTurnaround = new BooleanSetting("Elytra Turnaround", this, true);
    public final BooleanSetting visualElytraRotation = new BooleanSetting("Visual Elytra Rotation", this, true);
    public final BooleanSetting useResolver = new BooleanSetting("Resolver (Elytra)", this, true);

    
    public final BooleanSetting smartCrits = new BooleanSetting("Smart Crits", this, true);
    public final BooleanSetting sprintReset = new BooleanSetting("Sprint Reset", this, true);
    public final BooleanSetting attackWhileEating = new BooleanSetting("Attack While Eating", this, false);

    private ElytraTarget elytraTarget;

    private final ReallyWorldRotation reallyWorldRotation = new ReallyWorldRotation(this);
    private final SmoothRotation smoothRotation = new SmoothRotation(this);
    private final LegitRotation legitRotation = new LegitRotation(this);
    private final NeuroRotation neuroRotation = new NeuroRotation(this);
    private final FuntimeRotation funtimeRotation = new FuntimeRotation(this);
    private final FtTestRotation ftTestRotation = new FtTestRotation(this);
    private final FtTestFovRotation ftTestFovRotation = new FtTestFovRotation(this);
    public boolean isResolving = false;
    public Vec3d resolverPoint = null;
    private final StopWatch resolverTimer = new StopWatch();

    public boolean isTurnaroundActive = false;
    public boolean isSlowdownActive = false;
    private LivingEntity target;
    public static LivingEntity lastTarget;

    
    public int ticksToAttack = 0;
    public float preddict;

    
    private boolean needSprintReset = false;
    private boolean sprintResetDone = false;
    private int sprintResetTicks = 0;

    public AttackAura() {
        addSettings(rotation, targets, distance, preRotation,rotationBehavior,throughWalls, smartAim,rotationSpeed,renderFov, fovRadius,
                predictate, predictValue, elytraSlowdown, slowdownMode, slowdownRadius, minSpeed, preHitTicks,
                hitAfterOvertake, moveFix, onlySpace, clientLook, showPredictPoint, elytraTurnaround,
                visualElytraRotation, useResolver, smartCrits, sprintReset, attackWhileEating);
        targets.select("Players");
        targets.select("Naked");
        targets.select("Monsters");
        targets.select("Animals");
    }

    public LivingEntity getTarget() {
        return target;
    }

    private FreeCam getFreeCam() {
        return Cheap.getInstance().getModuleStorage().get(FreeCam.class);
    }

    private PlayerEntity fakePlayerOrSelf() {
        FreeCam freeCam = getFreeCam();
        OtherClientPlayerEntity fakePlayer = freeCam != null ? freeCam.getFakePlayer() : null;
        return fakePlayer != null ? fakePlayer : mc.player;
    }

    private ElytraTarget getElytraTarget() {
        if (this.elytraTarget == null) {
            this.elytraTarget = Cheap.getInstance().getModuleStorage().get(ElytraTarget.class);
        }
        return this.elytraTarget;
    }

    private void findResolverPoint() {
        if (mc.player == null || mc.world == null) return;
        Vec3d eye = mc.player.getEyePos();

        float oppositeYaw = mc.player.getYaw() + 180f;

        float searchPitch = -50f;

        int[] yawOffsets = {0, 30, -30, 45, -45, 60, -60, 90, -90};

        for (int offset : yawOffsets) {
            float testYaw = oppositeYaw + offset;

            float radYaw = (float) Math.toRadians(testYaw);
            float radPitch = (float) Math.toRadians(searchPitch);

            double x = -Math.sin(radYaw) * Math.cos(radPitch);
            double y = -Math.sin(radPitch);
            double z = Math.cos(radYaw) * Math.cos(radPitch);

            Vec3d checkVec = new Vec3d(x, y, z).normalize().multiply(8.0);
            Vec3d endPoint = eye.add(checkVec);

            if (mc.world.raycast(new RaycastContext(eye, endPoint, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS) {
                resolverPoint = endPoint;
                return;
            }
        }
        resolverPoint = null;
    }

    @Subscribe
    public void onGameUpdate(EventUpdate e) {
        if (mc.player == null || target == null) return;

        if (AIRotationRecorder.isRecording()) {
            return;
        }

        if (isResolving) {
            if (resolverTimer.finished(300)) {
                isResolving = false;
            } else if (resolverPoint != null) {
                var rot = new Rotation(new RotationUtil().calculate(resolverPoint));
                RotationComponent.update(rot, 360, 360, 360, 360, 0, 1, clientLook.getValue());
                smoothRotation.setLastRotation(rot);
                return;
            }
        }

        if (rotationBehavior.is("Snaps")) {
            if(isTargetInFov(target)){
                boolean isReadyToAttack = mc.player.getAttackCooldownProgress(1.0f) >= 0.95f && ticksToAttack <= 0;
                if (!isReadyToAttack ) {
                    return;
                }
            }else {
                return;
            }
        }

        boolean playerOnElytra = mc.player.isGliding();
        if (playerOnElytra && (this.elytraTarget == null || !this.elytraTarget.isEnabled())) {
            return;
        }

        switch (rotation.getValue()) {
            case "ReallyWorld" -> reallyWorldRotation.update(target);
            case "Smooth" -> smoothRotation.update(target);
            case "Neuro" -> neuroRotation.update(target);
            case "FtNew" -> funtimeRotation.update(target);
            case "FtTest" -> ftTestRotation.update(target);
            case "FtFestfov" -> ftTestFovRotation.update(target);
            case "Legit"  ->{
                if(isTargetInFov(target)){
                    legitRotation.update(target);
                }else{
                    return;
                }
            }
        }
    }

    @Subscribe
    public void onHudRender(EventHud e) {
        if (mc.player == null || mc.world == null) return;

        if (!renderFov.getValue()) return;
        if(rotationBehavior.getValue().equals("Snaps") || rotation.getValue().equals("Legit")){
        float radius = fovRadius.getFloatValue();
        float halfFov = mc.options.getFov().getValue() / 2.0f;
        float fovRatio = radius / halfFov;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f;
        float circleRadius = (screenHeight / 2.0f) * fovRatio;

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5F);

        Matrix4f matrix = e.getDrawContext().getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        float halfRadius = fovRadius.getFloatValue() / 2.0f;
        int segments = 64;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            float x = centerX + (float) Math.cos(angle) * (circleRadius /2);
            float y = centerY + (float) Math.sin(angle) * (circleRadius/2);
            buffer.vertex(matrix, x, y, 0.0f).color(1.0f, 1.0f, 1.0f, 0.8f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        }
    }
    @Subscribe
    public void onSprint(EventSprint e) {
        if (canStopSprinting()) e.cancel();
    }

    @Subscribe
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null) return;

        
        if (needSprintReset) {
            event.setForward(0);
            event.setStrafe(0);
            mc.player.setSprinting(false);
            needSprintReset = false;
            sprintResetDone = true;
            sprintResetTicks = 0;
            return;
        }

        if (!moveFix.is("Targeted")) return;
        if (target == null) return;

        if (mc.player.isGliding()) {
            event.setForward(0);
            event.setStrafe(0);
            return;
        }

        if (event.getForward() == 0 && event.getStrafe() == 0) return;

        float yaw = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw());

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        double targetAngle = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);

        float bestForward = 0, bestStrafe = 0;
        float smallestDiff = Float.MAX_VALUE;
        for (float f = -1f; f <= 1f; f++) {
            for (float s = -1f; s <= 1f; s++) {
                if (f == 0 && s == 0) continue;
                double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(RotationComponent.direction(yaw, f, s)));
                float diff = (float) Math.abs(MathHelper.wrapDegrees((float) (targetAngle - predictedAngle)));
                if (diff < smallestDiff) {
                    smallestDiff = diff;
                    bestForward = f;
                    bestStrafe = s;
                }
            }
        }

        event.setForward(bestForward);
        event.setStrafe(bestStrafe);
    }

    @Subscribe
    public void onTickPost(EventUpdate ignored) {
        if (mc.player == null || mc.world == null) return;

        
        if (ticksToAttack > 0) ticksToAttack--;

        
        if (sprintResetDone) sprintResetTicks++;

        updateTarget();

        if (target != null) {
            lastTarget = target;
            Vec3d predict = PredictUtils.predict(target, predictValue.getValue());
            double distToPredict = mc.player.getEyePos().distanceTo(predict);

            if (elytraSlowdown.getValue() && mc.player.isGliding()) {
                if (slowdownMode.is("Before Hit")) {
                    isSlowdownActive = ticksToAttack <= preHitTicks.getValue();
                } else {
                    isSlowdownActive = distToPredict < 2.7 && ticksToAttack <= 2;
                }
            } else {
                isSlowdownActive = false;
            }

            if (!shouldAttack()) return;

            if (sprintReset.getValue() && !sprintResetDone) {
                needSprintReset = true;
                Sprint sprint = Cheap.getInstance().getModuleStorage().get(Sprint.class);
                if (sprint != null && sprint.isEnabled()) {
                    Sprint.pushPause(0);
                }
                return;
            }

            if (sprintReset.getValue() && sprintResetDone && sprintResetTicks < 1) {
                return;
            }

            if (useResolver.getValue() && mc.player.isGliding()) {
                mc.player.setVelocity(0, 0, 0);
                findResolverPoint();
                if (resolverPoint != null) {
                    isResolving = true;
                    resolverTimer.reset();
                }
            }
            if (sprintReset.getValue()) {
                Sprint.popPause();
            }

            ticksToAttack = 10;

            sprintResetDone = false;
            sprintResetTicks = 0;

            assert mc.interactionManager != null;
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);

        } else {
            if (needSprintReset || sprintResetDone) {
                Sprint.popPause();
            }
            needSprintReset = false;
            sprintResetDone = false;
            sprintResetTicks = 0;
        }
    }

    private boolean shouldAttack() {
        if (mc.player.getAttackCooldownProgress(1.5f) < 0.93f) return false;

        if (!attackWhileEating.getValue() && mc.player.isUsingItem() && !mc.player.isBlocking()) {
            return false;
        }

        
        if (smartCrits.getValue()) {
            if (!IdealHitUtils.canCritical(target)) return false;
        }

        PlayerEntity player = fakePlayerOrSelf();

        double currentDist = player.getEyePos().distanceTo(BestPoint.getNearestPoint(target));
        if (currentDist > distance.getValue()) return false;

        if (!player.canSee(target)) return false;

        if (!RayTraceUtil.rayTrace(player.getRotationVector(), distance.getValue(), target.getBoundingBox())) return false;

        return true;
    }
    private boolean isTargetInFov(LivingEntity target) {
        if (mc.player == null || target == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = target.getBoundingBox().getCenter();
        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        float yawDiff = Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
        float pitchDiff = Math.abs(pitch - mc.player.getPitch());
        float halfRadius = fovRadius.getFloatValue() / 2.0f;
        float gameFov = mc.options.getFov().getValue();
        float yawLimit = halfRadius;
        float pitchLimit = halfRadius * (gameFov > 0 ? (mc.getWindow().getScaledHeight() / (float) mc.getWindow().getScaledWidth()) : 0.6F);
        if (yawDiff > yawLimit) return false;
        if (pitchDiff > pitchLimit) return false;
        double realDist = eyePos.distanceTo(targetPos);
        if (realDist > distance.getFloatValue()) return false;
        if (!throughWalls.getValue() && !mc.player.canSee(target)) return false;
        return true;
    }
    private boolean isValidEntity(Entity entity) {
        if (!entity.isAlive()) return false;

        FreeCam freeCam = getFreeCam();
        OtherClientPlayerEntity fakePlayer = freeCam != null ? freeCam.getFakePlayer() : null;
        PlayerEntity player = fakePlayer != null ? fakePlayer : mc.player;
        if (entity == fakePlayer) return false;
        if (entity instanceof ClientPlayerEntity) return false;
        if (entity instanceof ArmorStandEntity) return false;
        if (entity instanceof PlayerEntity p) {
            boolean hasArmor = false;
            for (ItemStack stack : p.getArmorItems()) {
                if (!stack.isEmpty()) {
                    hasArmor = true;
                    break;
                }
            }
            if (hasArmor && !targets.isSelected("Players")) return false;
            if (!hasArmor && !targets.isSelected("Naked")) return false;
            if (Cheap.getInstance().getFriendManager().contains(p.getNameForScoreboard())) return false;
        }
        if ((entity instanceof HostileEntity || entity instanceof AmbientEntity) && !targets.isSelected("Monsters"))
            return false;
        if ((entity instanceof PassiveEntity || entity instanceof FishEntity) && !targets.isSelected("Animals"))
            return false;
        if (player.getEyePos().distanceTo(BestPoint.getNearestPoint(entity)) > (player.isGliding() ? 50 : distance.getValue() + preRotation.getValue()))
            return false;
        return true;
    }

    public boolean canAttack() {
        return shouldAttack();
    }

    public boolean canStopSprinting() {
        if (target == null) return false;
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.93f) return false;
        if (ticksToAttack > 1) return false;
        if (mc.player.fallDistance == 0 && !(mc.player.isOnGround() && mc.options.jumpKey.isPressed())) return false;
        return true;
    }

    private void updateTarget() {
        LivingEntity best = null;
        double bestFovDot = -1;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity) {
                if (!isValidEntity(entity)) continue;

                Vec3d targetVec = BestPoint.getNearestPoint(entity).subtract(eyePos).normalize();
                double dot = lookVec.dotProduct(targetVec);

                if (dot > bestFovDot) {
                    bestFovDot = dot;
                    best = (LivingEntity) entity;
                }
            }
        }

        if (target == null || !isValidEntity(target)) {
            this.target = best;
        }
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (!showPredictPoint.getValue()) return;
        if (target == null || !target.isGliding()) return;

        renderPredictPoint(event.getMatrices(), event.getCamera().getPos());
    }

    private void renderPredictPoint(MatrixStack matrices, Vec3d cam) {
        Vec3d predictPos = PredictUtils.predict(target, predictValue.getValue());

        float size = 0.35f;
        int color = ColorProvider.getColorClient();

        matrices.push();
        matrices.translate(predictPos.x - cam.x, predictPos.y - cam.y, predictPos.z - cam.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 1;

        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, -size).color(r, g, b, a);

        buffer.vertex(matrix, size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, size).color(r, g, b, a);

        buffer.vertex(matrix, size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, -size, size).color(r, g, b, a);

        buffer.vertex(matrix, -size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, a);

        buffer.vertex(matrix, -size, size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, size, size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, size).color(r, g, b, a);

        buffer.vertex(matrix, size, size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, size).color(r, g, b, a);

        buffer.vertex(matrix, -size, size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, size).color(r, g, b, a);

        buffer.vertex(matrix, -size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, size).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    @Override
    public void onEnable() {
        target = null;
        elytraTarget = null;
        ticksToAttack = 0;
        needSprintReset = false;
        sprintResetDone = false;
        sprintResetTicks = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        
        if (needSprintReset || sprintResetDone) {
            Sprint.popPause();
        }
        target = null;
        ticksToAttack = 0;
        isResolving = false;
        resolverPoint = null;
        elytraTarget = null;
        needSprintReset = false;
        sprintResetDone = false;
        sprintResetTicks = 0;
        super.onDisable();
    }
}