package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "Trails", moduleDesc = "Renders a trail behind you", moduleCategory = ModuleCategory.RENDER)
public class Trails extends Module {

    private final SliderSetting duration = new SliderSetting("Duration", this, 500, 100, 3000, 10);
    private final BooleanSetting gradientColor = new BooleanSetting("Gradient Color", this, false);
    private final BooleanSetting rainbowColor = new BooleanSetting("Rainbow", this, false).visible(gradientColor::getValue);
    private final List<Point> points = new ArrayList<>();

    public Trails() {
        addSetting(duration);
        addSetting(gradientColor);
        addSetting(rainbowColor);
    }

    @Override
    public void onDisable() {
        points.clear();
        super.onDisable();
    }

    @Subscribe
    public void onRender(EventWorldRenderer e) {
        if (mc.player == null || mc.world == null || mc.options.getPerspective().isFirstPerson()) return;

        long currentTime = System.currentTimeMillis();
        float tickDelta = e.getRenderTickCounter().getTickDelta(true);

        points.removeIf(p -> (currentTime - p.time) > duration.getValue());

        Vec3d pos = new Vec3d(
                MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX()),
                MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY()),
                MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ())
        );
        points.add(new Point(pos));

        if (points.size() < 2) return;

        renderTrail(e.getMatrices(), e.getCamera().getPos());
    }

    private void renderTrail(MatrixStack ms, Vec3d cam) {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        ms.push();

        Matrix4f matrix = ms.peek().getPositionMatrix();

        int themePrimary = ThemeManager.getInstance().getPrimary();
        int themeSecondary = ThemeManager.getInstance().getSecondary();

        float playerH = mc.player.getHeight();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int idx = 0;
        for (Point p : points) {
            float progress = (float) idx / (float) (points.size() - 1);
            float alpha = (float) idx / (float) points.size() * 0.7f;
            int a = (int) (alpha * 255);

            int pointColor = getPointColor(progress, themePrimary, themeSecondary);
            int pr = (pointColor >> 16) & 0xFF;
            int pg = (pointColor >> 8) & 0xFF;
            int pb = pointColor & 0xFF;

            buffer.vertex(matrix, (float) (p.pos.x - cam.x), (float) (p.pos.y + playerH - cam.y), (float) (p.pos.z - cam.z))
                    .color(pr, pg, pb, a);
            buffer.vertex(matrix, (float) (p.pos.x - cam.x), (float) (p.pos.y - cam.y), (float) (p.pos.z - cam.z))
                    .color(pr, pg, pb, a);
            idx++;
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(2);

        renderLineStrip(matrix, points, true, themePrimary, themeSecondary, cam);
        renderLineStrip(matrix, points, false, themePrimary, themeSecondary, cam);

        ms.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderLineStrip(Matrix4f matrix, List<Point> points, boolean top, int themePrimary, int themeSecondary, Vec3d cam) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        float playerH = mc.player.getHeight();
        int idx = 0;
        for (Point p : points) {
            float progress = (float) idx / (float) (points.size() - 1);
            float alpha = Math.min((float) idx / (float) points.size() * 1.5f, 1f);
            int a = (int) (alpha * 255);
            float y = top ? (float) (p.pos.y + playerH - cam.y) : (float) (p.pos.y - cam.y);

            int pointColor = getPointColor(progress, themePrimary, themeSecondary);
            int pr = (pointColor >> 16) & 0xFF;
            int pg = (pointColor >> 8) & 0xFF;
            int pb = pointColor & 0xFF;

            buffer.vertex(matrix, (float) (p.pos.x - cam.x), y, (float) (p.pos.z - cam.z))
                    .color(pr, pg, pb, a);
            idx++;
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    





    private int getPointColor(float progress, int themePrimary, int themeSecondary) {
        if (!gradientColor.getValue()) {
            return themePrimary;
        }
        if (rainbowColor.getValue()) {
            float hue = progress * 0.8f;
            return Color.HSBtoRGB(hue, 1.0f, 1.0f);
        }
        
        return interpolateColor(themeSecondary, themePrimary, progress);
    }

    


    private int interpolateColor(int color1, int color2, float t) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static class Point {
        public Vec3d pos;
        public long time;

        Point(Vec3d pos) {
            this.pos = pos;
            this.time = System.currentTimeMillis();
        }
    }
}