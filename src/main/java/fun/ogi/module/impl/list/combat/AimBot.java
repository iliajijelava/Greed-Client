package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.NumberSetting;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleInformation(moduleName = "AimBot", moduleDesc = "Auto-aim for bow and crossbow", moduleCategory = ModuleCategory.COMBAT)
public class AimBot extends Module {

    private final ListSetting targetTypes = new ListSetting("Targets", this, "Players", "Armored", "Unarmored", "Hostile", "Zombies");

    private final NumberSetting range = new NumberSetting("Range", this, 40.0, 10.0, 100.0, 1.0);
    private final NumberSetting aimTime = new NumberSetting("Aim Time (ticks)", this, 10.0, 0.0, 40.0, 1.0);
    private final BooleanSetting showCrosshair = new BooleanSetting("Show Crosshair", this, true);
    private final NumberSetting crosshairSize = new NumberSetting("Crosshair Size", this, 1.0, 0.3, 3.0, 0.1);

    private static final Identifier CROSSHAIR_TEXTURE = Identifier.of("cheap", "textures/cross/hit.png");

    private LivingEntity target;
    private boolean isAiming;
    private float aimProgress;
    private float targetYaw;
    private float targetPitch;

    public AimBot() {
        addSettings(targetTypes, range, aimTime, showCrosshair, crosshairSize);
        targetTypes.select("Players");
        targetTypes.select("Armored");
    }

    private boolean isHoldingBowOrCrossbow() {
        ItemStack mainHand = mc.player.getMainHandStack();
        ItemStack offHand = mc.player.getOffHandStack();
        return mainHand.getItem() instanceof BowItem ||
                mainHand.getItem() instanceof CrossbowItem ||
                offHand.getItem() instanceof BowItem ||
                offHand.getItem() instanceof CrossbowItem;
    }

    private boolean isUsingBowOrCrossbow() {
        return mc.player.isUsingItem() && isHoldingBowOrCrossbow();
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive() || entity.getHealth() <= 0) return false;

        if (entity instanceof PlayerEntity player) {
            if (!targetTypes.isSelected("Players")) return false;

            boolean hasArmor = false;
            for (ItemStack armor : player.getArmorItems()) {
                if (!armor.isEmpty()) {
                    hasArmor = true;
                    break;
                }
            }

            if (targetTypes.isSelected("Armored") && hasArmor) return true;
            if (targetTypes.isSelected("Unarmored") && !hasArmor) return true;
            if (!targetTypes.isSelected("Armored") && !targetTypes.isSelected("Unarmored")) return true;

            return false;
        }

        if (entity instanceof ZombieEntity) {
            return targetTypes.isSelected("Zombies");
        }

        if (entity instanceof HostileEntity) {
            return targetTypes.isSelected("Hostile");
        }

        return false;
    }

    private LivingEntity findBestTarget() {
        List<LivingEntity> targets = new ArrayList<>();
        Box searchBox = mc.player.getBoundingBox().expand(range.getFloatValue());

        for (LivingEntity entity : mc.world.getEntitiesByClass(LivingEntity.class, searchBox, e -> true)) {
            if (!isValidTarget(entity)) continue;
            if (mc.player.distanceTo(entity) > range.getFloatValue()) continue;
            targets.add(entity);
        }

        if (targets.isEmpty()) return null;

        targets.sort(Comparator.comparingDouble(entity -> mc.player.distanceTo(entity)));
        return targets.get(0);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        isAiming = isUsingBowOrCrossbow();

        if (isAiming) {
            LivingEntity newTarget = findBestTarget();

            if (newTarget != null) {
                if (target != newTarget) {
                    target = newTarget;
                    aimProgress = 0f;
                }

                Vec3d eyes = mc.player.getEyePos();
                Vec3d targetPos = target.getBoundingBox().getCenter();
                double dx = targetPos.x - eyes.x;
                double dy = targetPos.y - eyes.y;
                double dz = targetPos.z - eyes.z;
                double distance = Math.sqrt(dx * dx + dz * dz);

                float newYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
                float newPitch = (float) -Math.toDegrees(Math.atan2(dy, distance));

                float maxStep = 1f / Math.max(1f, aimTime.getFloatValue());
                aimProgress = Math.min(aimProgress + maxStep, 1f);

                float currentYaw = mc.player.getYaw();
                float currentPitch = mc.player.getPitch();

                float yawDiff = MathHelper.wrapDegrees(newYaw - currentYaw);
                float pitchDiff = newPitch - currentPitch;

                targetYaw = currentYaw + yawDiff * aimProgress;
                targetPitch = currentPitch + pitchDiff * aimProgress;

                mc.player.setYaw(targetYaw);
                mc.player.setPitch(targetPitch);
            }
        } else {
            target = null;
            aimProgress = 0f;
        }
    }

    @Subscribe
    public void onRender3D(EventWorldRenderer event) {
        if (!showCrosshair.getValue() || target == null || !isAiming) return;

        float tickDelta = event.getRenderTickCounter().getTickDelta(false);
        double ex = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX());
        double ey = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()) + target.getHeight() / 2.0;
        double ez = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ());
        Vec3d cam = event.getCamera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, CROSSHAIR_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        MatrixStack ms = new MatrixStack();
        ms.translate(ex - cam.x, ey - cam.y, ez - cam.z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

        float size = crosshairSize.getFloatValue() * 0.5f;
        int alpha = (int) (255 * aimProgress);
        int color = ThemeManager.getInstance().getPrimary();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        Matrix4f matrix = ms.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buffer.vertex(matrix, -size, -size, 0).texture(0, 1).color(r, g, b, alpha);
        buffer.vertex(matrix, -size, size, 0).texture(0, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, size, size, 0).texture(1, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, size, -size, 0).texture(1, 1).color(r, g, b, alpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        isAiming = false;
        aimProgress = 0f;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        isAiming = false;
        aimProgress = 0f;
    }
}

