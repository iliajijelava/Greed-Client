package fun.ogi.util.render.renderers.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.providers.ResourceProvider;
import fun.ogi.util.render.renderers.IRenderer;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

public record BuiltBorder(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float thickness,
        float internalSmoothness, float externalSmoothness
) implements IRenderer {

    private static final ShaderProgramKey RECTANGLE_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("border"),
            VertexFormats.POSITION_COLOR, Defines.EMPTY);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float width = this.size.width(), height = this.size.height();
        ShaderProgram shader = RenderSystem.setShader(RECTANGLE_SHADER_KEY);
        GlUniform size = shader.getUniform("Size");
        GlUniform radius = shader.getUniform("Radius");
        GlUniform thickness = shader.getUniform("Thickness");
        GlUniform smoothness = shader.getUniform("Smoothness");
        if (size != null) size.set(width, height);
        if (radius != null) radius.set(this.radius.radius1(), this.radius.radius2(),
                this.radius.radius3(), this.radius.radius4());
        if (thickness != null) thickness.set(this.thickness);
        if (smoothness != null) smoothness.set(this.internalSmoothness, this.externalSmoothness);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x, y, z).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).color(this.color.color4());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

}