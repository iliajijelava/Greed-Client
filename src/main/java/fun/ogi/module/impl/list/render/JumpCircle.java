package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.NumberSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.StopWatch;
import fun.ogi.util.render.builders.impl.LiquidBuilder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInformation(moduleName = "JumpCircles", moduleDesc = "Adds a circle effect when jumping.", moduleCategory = ModuleCategory.RENDER)
public class JumpCircle extends Module {
    private final List<Circle> circles = new ArrayList<>();
    private final Identifier circleTexture = Identifier.of("cheap", "textures/circle2.png");
    private final Identifier bloomTexture = Identifier.of("cheap", "textures/bloom.png");
    private final Identifier secondCircleTexture = Identifier.of("cheap","textures/circle3.png");
    private final NumberSetting maxSize = new NumberSetting("Max Size", this, 2.5, 1.0, 3.0, 0.1);
    private final NumberSetting speed = new NumberSetting("Speed", this, 1000, 500, 5000, 100);
    private final ModeSetting circleMod = new ModeSetting("Circle mod", this, "2D", "2D", "3D","Test");
    private final ModeSetting twoDCircleStyle = new ModeSetting("2D Circle Style",this,"First","First","Second");

    
    private final NumberSetting liquidGlow = new NumberSetting("Liquid Glow", this, 0.8, 0.0, 1.0, 0.05);
    private final NumberSetting liquidSegments = new NumberSetting("Liquid Segments", this, 32, 16, 64, 4);

    private boolean prevOnGround = true;
    private double groundY;
    private final Random random = new Random();

    public JumpCircle() {
        addSettings(maxSize, speed, circleMod, twoDCircleStyle, liquidGlow, liquidSegments);
    }

    @Subscribe
    public void onTick(EventUpdate e) {
        if (mc.player == null) return;

        boolean onGround = mc.player.isOnGround();

        if (onGround) {
            groundY = mc.player.getY();
        }

        if (prevOnGround && !onGround) {
            Vec3d pos = new Vec3d(
                    mc.player.getX(),
                    groundY + 0.05,
                    mc.player.getZ()
            );

            StopWatch timer = new StopWatch();
            timer.reset();

            circles.add(
                    new Circle(
                            pos,
                            timer,
                            generateHeights()
                    )
            );
        }

        prevOnGround = onGround;
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer e) {
        if (circles.isEmpty()) return;

        circles.removeIf(c -> c.timer.hasTimePassed((long) speed.getValue()));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Tessellator tessellator = Tessellator.getInstance();
        MatrixStack matrices = e.getMatrices();
        Vec3d cam = e.getCamera().getPos();

        for (Circle circle : circles) {
            switch (circleMod.getValueAsString()) {
                case "2D" -> renderSingleCircle(tessellator, matrices, circle, cam);
                case "Test" -> renderLiquidCircle(tessellator, matrices, circle, cam); 
                case "3D" -> render3DCircle(tessellator, matrices, circle, cam);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    


    


    private void renderLiquidCircle(Tessellator tessellator, MatrixStack matrices, Circle circle, Vec3d cam) {
        float lifeTime = circle.timer.getElapsedTime();
        float maxTime = (float) speed.getValue();
        float progress = Math.min(lifeTime / maxTime, 1f);

        float maxRadius = (float) maxSize.getValue();
        float currentRadius = progress * maxRadius;
        float alpha = 1f - progress;

        int primary = ThemeManager.getInstance().getPalette().getPrimary();
        float r = ((primary >> 16) & 255) / 255f;
        float g = ((primary >> 8) & 255) / 255f;
        float b = (primary & 255) / 255f;

        float glow = (float) liquidGlow.getValue();

        matrices.push();
        matrices.translate(circle.pos.x - cam.x, circle.pos.y - cam.y, circle.pos.z - cam.z);

        
        

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE
        );

        int segments = (int) liquidSegments.getValue();

        
        for (int layer = 0; layer < 3; layer++) {
            float layerScale = 1f + layer * 0.2f;
            float layerAlpha = alpha * (1f - layer * 0.3f) * glow;
            float layerRadius = currentRadius * layerScale;

            renderLiquidLayer(tessellator, matrix, circle, progress, layerRadius,
                    r, g, b, layerAlpha, segments, layer);
        }

        
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, bloomTexture);

        BufferBuilder bloomBuffer = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR
        );

        renderBloomLayer(bloomBuffer, matrix, currentRadius * 0.8f, r, g, b, alpha * 0.5f);
        BufferRenderer.drawWithGlobalProgram(bloomBuffer.end());

        RenderSystem.defaultBlendFunc();
        matrices.pop();
    }

    


    private void renderLiquidLayer(Tessellator tessellator, Matrix4f matrix, Circle circle,
                                   float progress, float radius, float r, float g, float b, float alpha,
                                   int segments, int layerIndex) {

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        
        BufferBuilder buffer = tessellator.begin(
                VertexFormat.DrawMode.TRIANGLE_FAN,
                VertexFormats.POSITION_COLOR
        );

        
        float yOffset = 0.01f + layerIndex * 0.005f;
        buffer.vertex(matrix, 0, yOffset, 0).color(r, g, b, alpha);

        
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(i * 360.0 / segments);

            
            float wave = 0;
            if (progress > 0.1f) {
                float waveIntensity = circle.heights[i % circle.heights.length] * 0.15f;
                wave = (float) Math.sin(angle * 6 + progress * Math.PI * 3) * waveIntensity;
            }

            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            
            float y = yOffset + wave;

            
            float edgeAlpha = alpha * (0.8f + wave * 2f);
            edgeAlpha = Math.max(edgeAlpha, alpha * 0.3f);

            buffer.vertex(matrix, x, y, z).color(r, g, b, edgeAlpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        
        if (layerIndex == 0) {
            renderLiquidBorder(tessellator, matrix, radius, r, g, b, alpha * 1.2f, segments, yOffset);
        }
    }

    


    private void renderLiquidBorder(Tessellator tessellator, Matrix4f matrix, float radius,
                                    float r, float g, float b, float alpha, int segments, float yOffset) {

        BufferBuilder buffer = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR
        );

        float borderWidth = 0.04f;
        float innerRadius = radius - borderWidth;
        float highlight = Math.min(1f, (r + g + b) / 3f + 0.4f);

        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians(i * 360.0 / segments);
            double angle2 = Math.toRadians((i + 1) * 360.0 / segments);

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            float x1 = cos1 * radius;
            float z1 = sin1 * radius;
            float x2 = cos2 * radius;
            float z2 = sin2 * radius;
            float x3 = cos2 * innerRadius;
            float z3 = sin2 * innerRadius;
            float x4 = cos1 * innerRadius;
            float z4 = sin1 * innerRadius;

            
            buffer.vertex(matrix, x1, yOffset + 0.015f, z1).color(highlight, highlight, highlight, alpha);
            buffer.vertex(matrix, x2, yOffset + 0.015f, z2).color(highlight, highlight, highlight, alpha);
            buffer.vertex(matrix, x3, yOffset + 0.01f, z3).color(r, g, b, alpha * 0.7f);
            buffer.vertex(matrix, x4, yOffset + 0.01f, z4).color(r, g, b, alpha * 0.7f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    


    private void renderBloomLayer(BufferBuilder buffer, Matrix4f matrix, float radius,
                                  float r, float g, float b, float alpha) {

        float yOffset = 0.005f;
        float hs = radius;

        
        buffer.vertex(matrix, -hs, yOffset, -hs).texture(0, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, -hs, yOffset, hs).texture(0, 1).color(r, g, b, alpha);
        buffer.vertex(matrix, hs, yOffset, hs).texture(1, 1).color(r, g, b, alpha);
        buffer.vertex(matrix, hs, yOffset, -hs).texture(1, 0).color(r, g, b, alpha);
    }
    

    private void renderSingleCircle(Tessellator tessellator, MatrixStack matrices, Circle circle, Vec3d cam) {
        float lifeTime = circle.timer.getElapsedTime();
        float maxTime = (float) speed.getValue();
        float progress = Math.min(lifeTime / maxTime, 1f);
        float size = progress * (float) maxSize.getValue();
        float alpha = 1f - progress;

        int primary = ThemeManager.getInstance().getPalette().getPrimary();
        float r = ((primary >> 16) & 255) / 255f;
        float g = ((primary >> 8) & 255) / 255f;
        float b = (primary & 255) / 255f;

        matrices.push();
        matrices.translate(circle.pos.x - cam.x, circle.pos.y - cam.y, circle.pos.z - cam.z);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0,
                twoDCircleStyle.getValueAsString().equals("Second") ? secondCircleTexture : circleTexture);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float hs = size / 2f;

        BufferBuilder buffer = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR
        );

        buffer.vertex(matrix, -hs, 0, -hs).texture(0, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, -hs, 0, hs).texture(0, 1).color(r, g, b, alpha);
        buffer.vertex(matrix, hs, 0, hs).texture(1, 1).color(r, g, b, alpha);
        buffer.vertex(matrix, hs, 0, -hs).texture(1, 0).color(r, g, b, alpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        matrices.pop();
    }

    private void render3DCircle(Tessellator tessellator, MatrixStack matrices, Circle circle, Vec3d cam) {
        
        float lifeTime = circle.timer.getElapsedTime();
        float maxTime = (float) speed.getValue();

        float progress = Math.min(lifeTime / maxTime, 1f);

        int primary = ThemeManager.getInstance().getPalette().getPrimary();

        float r = ((primary >> 16) & 255) / 255f;
        float g = ((primary >> 8) & 255) / 255f;
        float b = (primary & 255) / 255f;

        float radius = progress * (float) maxSize.getValue();
        float alpha = 1f - progress;
        float thickness = 0.05f;

        matrices.push();

        matrices.translate(circle.pos.x - cam.x, circle.pos.y - cam.y, circle.pos.z - cam.z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        renderGlow(tessellator, matrix, circle, progress, radius, thickness, r, g, b, alpha);

        RenderSystem.defaultBlendFunc();

        BufferBuilder buffer = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR
        );

        drawRingGeometry(buffer, matrix, circle, progress, radius, thickness, r, g, b, alpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    private void renderGlow(Tessellator tessellator, Matrix4f matrix, Circle circle,
                            float progress, float radius, float thickness, float r, float g, float b, float baseAlpha) {

        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE
        );

        int glowLayers = 4;

        for (int layer = 1; layer <= glowLayers; layer++) {
            float scale = 1f + layer * 0.18f;
            float layerAlpha = baseAlpha * (0.35f / layer);

            BufferBuilder buffer = tessellator.begin(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_COLOR
            );

            drawRingGeometry(buffer, matrix, circle, progress, radius * scale,
                    thickness * scale * 2f, r, g, b, layerAlpha);

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private void drawRingGeometry(BufferBuilder buffer, Matrix4f matrix, Circle circle,
                                  float progress, float radius, float thickness, float r, float g, float b, float alpha) {

        int segments = 64;

        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians(i * 360.0 / segments);
            double angle2 = Math.toRadians((i + 1) * 360.0 / segments);

            float outerRadius = radius;
            float innerRadius = radius - thickness;

            float x1 = (float) Math.cos(angle1) * outerRadius;
            float z1 = (float) Math.sin(angle1) * outerRadius;

            float x2 = (float) Math.cos(angle2) * outerRadius;
            float z2 = (float) Math.sin(angle2) * outerRadius;

            float x3 = (float) Math.cos(angle2) * innerRadius;
            float z3 = (float) Math.sin(angle2) * innerRadius;

            float x4 = (float) Math.cos(angle1) * innerRadius;
            float z4 = (float) Math.sin(angle1) * innerRadius;

            float height = 0f;

            if (progress > 0.2f) {
                float heightProgress = (progress - 0.2f) / 0.3f;
                height = circle.heights[i] * heightProgress;
            }

            buffer.vertex(matrix, x1, 0, z1).color(r, g, b, alpha);
            buffer.vertex(matrix, x2, 0, z2).color(r, g, b, alpha);
            buffer.vertex(matrix, x3, height, z3).color(r, g, b, alpha);
            buffer.vertex(matrix, x4, height, z4).color(r, g, b, alpha);
        }
    }

    private float[] generateHeights() {
        int segments = 64;
        float[] heights = new float[segments];

        for (int i = 0; i < segments; i++) {
            if (random.nextFloat() < 0.25f) {
                heights[i] = 0.2f + random.nextFloat() * 0.8f;
            } else {
                heights[i] = 0f;
            }
        }

        return heights;
    }

    private record Circle(Vec3d pos, StopWatch timer, float[] heights) {}
}