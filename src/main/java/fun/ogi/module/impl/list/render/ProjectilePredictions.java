package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


@ModuleInformation(moduleName = "Projectile Prediction", moduleDesc = "Sam znaesh shto delaet", moduleCategory = ModuleCategory.RENDER)
public class ProjectilePredictions extends Module {

    private final List<Vec3d> path = new ArrayList<>();
    private Vec3d hitPos;

    public ProjectilePredictions() {
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer e) {
        if (mc.player == null || mc.world == null) return;

        path.clear();
        hitPos = null;

        double velocity = getProjectileVelocity();
        if (velocity <= 0) return;

        Vec3d from = mc.player.getEyePos().add(mc.player.getRotationVec(1.0f).multiply(0.2));
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d motion = look.multiply(velocity).add(mc.player.getVelocity());

        double gravity = getProjectileGravity();
        double drag = getProjectileDrag();

        Vec3d pos = from;

        for (int i = 0; i < 200; i++) {
            path.add(pos);
            Vec3d next = pos.add(motion);

            RaycastContext ctx = new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult blockHit = mc.world.raycast(ctx);

            if (!blockHit.getType().equals(HitResult.Type.MISS)) {
                hitPos = blockHit.getPos();
                path.add(hitPos);
                break;
            }

            motion = motion.multiply(drag);
            motion = motion.add(0, -gravity, 0);
            pos = next;

            if (pos.y < -128) break;
        }

        if (path.size() < 2) return;

        int themeColor = ThemeManager.getInstance().getPrimary();
        Color c = new Color(themeColor);
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;

        MatrixStack ms = new MatrixStack();
        ms.push();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = ms.peek().getPositionMatrix();
        Vec3d cam = e.getCamera().getPos();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer;
        float alpha;

        RenderSystem.lineWidth(2.0f);
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < path.size() - 1; i++) {
            Vec3d p1 = path.get(i);
            Vec3d p2 = path.get(i + 1);
            alpha = 1.0f - (float) i / path.size() * 0.5f;
            buffer.vertex(matrix, (float) (p1.x - cam.x), (float) (p1.y - cam.y), (float) (p1.z - cam.z)).color(r, g, b, alpha);
            buffer.vertex(matrix, (float) (p2.x - cam.x), (float) (p2.y - cam.y), (float) (p2.z - cam.z)).color(r, g, b, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        if (hitPos != null) {
            int secColor = ThemeManager.getInstance().getSecondary();
            Color c2 = new Color(secColor);
            float r2 = c2.getRed() / 255f;
            float g2 = c2.getGreen() / 255f;
            float b2 = c2.getBlue() / 255f;

            float x = (float) (hitPos.x - cam.x);
            float y = (float) (hitPos.y - cam.y);
            float z = (float) (hitPos.z - cam.z);
            float s = 0.15f;

            float markerAlpha = 0.8f;

            RenderSystem.lineWidth(3.0f);
            buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            buffer.vertex(matrix, x - s, y, z).color(r2, g2, b2, markerAlpha);
            buffer.vertex(matrix, x + s, y, z).color(r2, g2, b2, markerAlpha);
            buffer.vertex(matrix, x, y - s, z).color(r2, g2, b2, markerAlpha);
            buffer.vertex(matrix, x, y + s, z).color(r2, g2, b2, markerAlpha);
            buffer.vertex(matrix, x, y, z - s).color(r2, g2, b2, markerAlpha);
            buffer.vertex(matrix, x, y, z + s).color(r2, g2, b2, markerAlpha);

            BufferRenderer.drawWithGlobalProgram(buffer.end());

            RenderSystem.lineWidth(2.0f);
            buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (int i = 0; i <= 360; i += 15) {
                double rad1 = Math.toRadians(i);
                double rad2 = Math.toRadians(i + 15);
                buffer.vertex(matrix, x + (float) (Math.cos(rad1) * s * 1.5), y, z + (float) (Math.sin(rad1) * s * 1.5)).color(r2, g2, b2, markerAlpha);
                buffer.vertex(matrix, x + (float) (Math.cos(rad2) * s * 1.5), y, z + (float) (Math.sin(rad2) * s * 1.5)).color(r2, g2, b2, markerAlpha);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        ms.pop();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private double getProjectileVelocity() {
        ItemStack stack = mc.player.getMainHandStack();

        if (stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
            return 3.0;
        }

        if (stack.getItem() instanceof BowItem && mc.player.isUsingItem()) {
            float charge = (mc.player.getItemUseTime() + mc.getRenderTickCounter().getTickDelta(false)) / 20f;
            charge = Math.min(charge, 1f);
            return charge * 3.0;
        }

        if (stack.getItem() instanceof TridentItem && mc.player.isUsingItem()) {
            return 2.5;
        }

        Item item = stack.getItem();

        if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem) {
            return 1.5;
        }

        if (item instanceof ExperienceBottleItem) {
            return 0.8;
        }

        if (item instanceof SplashPotionItem) {
            return 0.55;
        }

        return -1;
    }

    private double getProjectileGravity() {
        ItemStack stack = mc.player.getMainHandStack();
        Item item = stack.getItem();

        if (item instanceof SnowballItem || item instanceof EggItem || item instanceof EnderPearlItem) return 0.03;
        if (item instanceof ExperienceBottleItem) return 0.07;
        return 0.05;
    }

    private double getProjectileDrag() {
        return 0.99;
    }
}

