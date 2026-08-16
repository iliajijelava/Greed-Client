package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import org.joml.Matrix4f;

import java.awt.*;

@ModuleInformation(moduleName = "Chorus Radius", moduleDesc = "Shows radius of Chorus", moduleCategory = ModuleCategory.RENDER)
public class ChorusRadius extends Module {

    private static final float RADIUS = 8.0f;
    private static final int SEGMENTS = 120;

    private final BooleanSetting fill = new BooleanSetting("Fill", this, true);

    public ChorusRadius() {addSetting(fill);}

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.player == null || mc.world == null) return;

        boolean holdingItem = mc.player.getMainHandStack().isOf(Items.CHORUS_FRUIT)
                || mc.player.getOffHandStack().isOf(Items.CHORUS_FRUIT);
        if (!holdingItem) return;

        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();
        net.minecraft.util.math.Vec3d cam = event.getCamera().getPos();

        MatrixStack ms = new MatrixStack();
        ms.push();
        ms.translate(pX - cam.x, pY + 0.05 - cam.y, pZ - cam.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(2.5f);

        int themeColor = ThemeManager.getInstance().getPrimary();
        Color color = new Color(themeColor);
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();

        if (fill.getValue()) {
            BufferBuilder fillBuffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            fillBuffer.vertex(matrix, 0, 0, 0).color(r, g, b, 0.25f);
            for (int i = 0; i <= SEGMENTS; i++) {
                double angle = i * (Math.PI * 2.0) / SEGMENTS;
                float x = (float) (Math.cos(angle) * RADIUS);
                float z = (float) (Math.sin(angle) * RADIUS);
                fillBuffer.vertex(matrix, x, 0, z).color(r, g, b, 0.25f);
            }
            BufferRenderer.drawWithGlobalProgram(fillBuffer.end());

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        }

        BufferBuilder lineBuffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = i * (Math.PI * 2.0) / SEGMENTS;
            float x = (float) (Math.cos(angle) * RADIUS);
            float z = (float) (Math.sin(angle) * RADIUS);
            lineBuffer.vertex(matrix, x, 0, z).color(r, g, b, 1.0f);
        }
        BufferRenderer.drawWithGlobalProgram(lineBuffer.end());

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }
}