package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleInformation(moduleName = "China Hat", moduleDesc = "Renders a hat above your head.", moduleCategory = ModuleCategory.RENDER)
public class ChinaHat extends Module {
    private final SliderSetting size = new SliderSetting("Size", this, 1, 0.5, 2.0, 0.1);
    private final SliderSetting speed = new SliderSetting("Speed", this, 2.0, 0.0, 6.0, 0.5);
    private final SliderSetting height = new SliderSetting("Height", this, -0.1, -0.5, 1.5, 0.05);
    private final BooleanSetting friends = new BooleanSetting("Friends", this, true);
    private final BooleanSetting glow = new BooleanSetting("Glow", this, true);
    private final BooleanSetting throughWalls = new BooleanSetting("ThroughWalls", this, true);

    public ChinaHat() {
        addSettings(size, speed, height, friends, glow, throughWalls);
    }

    @Subscribe
    public void onRender(EventWorldRenderer e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.options.getPerspective().isFirstPerson()) return;

        float tickDelta = e.getRenderTickCounter().getTickDelta(true);
        MatrixStack ms = new MatrixStack();
        Vec3d cam = e.getCamera().getPos();

        double x = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
        double y = MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY());
        double z = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());

        ms.push();
        ms.translate(x - cam.x, y + mc.player.getHeight() + height.getFloatValue() - cam.y, z - cam.z);

        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, mc.player.prevHeadYaw, mc.player.headYaw);
        float pitch = MathHelper.lerp(tickDelta, mc.player.prevPitch, mc.player.getPitch());

        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-headYaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        float rotSpeed = speed.getFloatValue();
        if (rotSpeed > 0f) {
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((mc.player.age + tickDelta) * rotSpeed));
        }

        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        if (throughWalls.getValue()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float r = 0.7f * size.getFloatValue();
        float h = 0.35f * size.getFloatValue();
        int points = 64;

        int primary = ThemeManager.getInstance().getPalette().getPrimary();
        int secondary = ThemeManager.getInstance().getPalette().getSecondary();

        int pR = (primary >> 16) & 0xFF;
        int pG = (primary >> 8) & 0xFF;
        int pB = primary & 0xFF;

        int sR = (secondary >> 16) & 0xFF;
        int sG = (secondary >> 8) & 0xFF;
        int sB = secondary & 0xFF;

        
        BufferBuilder hat = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        hat.vertex(matrix, 0, h, 0).color(pR, pG, pB, 200);
        for (int i = 0; i <= points; i++) {
            double angle = Math.toRadians((360f / points) * i);
            float px = (float) Math.cos(angle) * r;
            float pz = (float) Math.sin(angle) * r;

            float t = gradientT(i, points);
            int cR = lerp(pR, sR, t);
            int cG = lerp(pG, sG, t);
            int cB = lerp(pB, sB, t);

            hat.vertex(matrix, px, 0, pz).color(cR, cG, cB, 90);
        }
        BufferRenderer.drawWithGlobalProgram(hat.end());

        if (glow.getValue()) {
            
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

            drawOutlinePass(tessellator, matrix, r, points, pR, pG, pB, sR, sG, sB, 8f, 35);
            drawOutlinePass(tessellator, matrix, r, points, pR, pG, pB, sR, sG, sB, 5f, 55);
            drawOutlinePass(tessellator, matrix, r, points, pR, pG, pB, sR, sG, sB, 3f, 90);

            RenderSystem.defaultBlendFunc();
        }

        
        drawOutlinePass(tessellator, matrix, r, points, pR, pG, pB, sR, sG, sB, 2.5f, 255);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        ms.pop();
    }

    private void drawOutlinePass(Tessellator tessellator, Matrix4f matrix, float r, int points,
                                 int pR, int pG, int pB, int sR, int sG, int sB,
                                 float lineWidth, int alpha) {
        RenderSystem.lineWidth(lineWidth);
        BufferBuilder outline = tessellator.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= points; i++) {
            double angle = Math.toRadians((360f / points) * i);
            float px = (float) Math.cos(angle) * r;
            float pz = (float) Math.sin(angle) * r;

            float t = gradientT(i, points);
            int cR = lerp(pR, sR, t);
            int cG = lerp(pG, sG, t);
            int cB = lerp(pB, sB, t);

            outline.vertex(matrix, px, 0, pz).color(cR, cG, cB, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(outline.end());
    }

    




    private float gradientT(int i, int points) {
        float progress = (i % points) / (float) points; 
        return 1f - Math.abs(progress * 2f - 1f); 
    }

    private int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}