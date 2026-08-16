package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

@ModuleInformation(moduleName = "DesYavka Overlay", moduleDesc = "Shows desorentation and yavka overlay.", moduleCategory = ModuleCategory.RENDER)
public class DesYavkaOverlay extends Module {
    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.player == null || mc.world == null) return;

        boolean holdingItem = mc.player.getMainHandStack().isOf(Items.ENDER_EYE) ||
                mc.player.getMainHandStack().isOf(Items.SUGAR);
        if (!holdingItem) return;

        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        MatrixStack ms = new MatrixStack();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();

        ms.push();
        ms.translate(pX - camPos.x, pY + 0.05 - camPos.y, pZ - camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(2.5f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        Entity target = null;
        if (mc.crosshairTarget instanceof EntityHitResult hit) {
            if (hit.getEntity() instanceof LivingEntity living && living != mc.player && !Cheap.getInstance().getFriendManager().contains(living.getName().getString())) {
                target = living;
            }
        }
        if (target != null && (target.isRemoved() || !target.isAlive() || mc.player.distanceTo(target) > 50 || Cheap.getInstance().getFriendManager().contains(target.getName().getString()))) {
            target = null;
        }

        boolean close = target != null && mc.player.distanceTo(target) <= 3;
        int themeColor = close ? ThemeManager.getInstance().getSecondary() : ThemeManager.getInstance().getPrimary();
        Color color = new Color(themeColor);
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        float radius = 8.0f;
        int segments = 120;
        Matrix4f matrix = ms.peek().getPositionMatrix();
        for (int i = 0; i <= segments; i++) {
            double angle = i * (Math.PI * 2.0) / segments;
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            buffer.vertex(matrix, x, 0, z).color(r, g, b, 1.0f);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }
}

