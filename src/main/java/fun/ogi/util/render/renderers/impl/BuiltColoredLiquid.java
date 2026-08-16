package fun.ogi.util.render.renderers.impl;

import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.providers.ResourceProvider;
import fun.ogi.util.render.renderers.IRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import java.awt.Color;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;

public record BuiltColoredLiquid(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float blurAmount
) implements IRenderer {

    private static final ShaderProgramKey COLORED_LIQUID_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("coloredliquid"),
            VertexFormats.POSITION_COLOR, Defines.EMPTY);

    private static final Supplier<SimpleFramebuffer> TEMP_FBO_SUPPLIER = Suppliers
            .memoize(() -> new SimpleFramebuffer(1920, 1024, false));
    private static final Framebuffer MAIN_FBO = MinecraftClient.getInstance().getFramebuffer();

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        SimpleFramebuffer fbo = TEMP_FBO_SUPPLIER.get();
        if (fbo.textureWidth != MAIN_FBO.textureWidth || fbo.textureHeight != MAIN_FBO.textureHeight) {
            fbo.resize(MAIN_FBO.textureWidth, MAIN_FBO.textureHeight);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        
        fbo.beginWrite(false);
        MAIN_FBO.draw(fbo.textureWidth, fbo.textureHeight);
        MAIN_FBO.beginWrite(false);

        RenderSystem.setShaderTexture(0, fbo.getColorAttachment());

        float width = this.size.width(), height = this.size.height();
        ShaderProgram shader = RenderSystem.setShader(COLORED_LIQUID_SHADER_KEY);

        if (shader != null) {
            if (shader.getUniform("Size") != null) {
                shader.getUniform("Size").set(width, height);
            }
            if (shader.getUniform("Radius") != null) {
                shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(),
                        this.radius.radius3(), this.radius.radius4());
            }
            if (shader.getUniform("BackColor") != null) {
                Color c = new Color(this.color.color1(), true);
                shader.getUniform("BackColor").set(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
            }
            if (shader.getUniform("BlurAmount") != null) {
                shader.getUniform("BlurAmount").set(this.blurAmount);
            }
        }

        int white = 0xFFFFFFFF;
        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x, y, z).color(white);
        builder.vertex(matrix, x, y + height, z).color(white);
        builder.vertex(matrix, x + width, y + height, z).color(white);
        builder.vertex(matrix, x + width, y, z).color(white);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

}

