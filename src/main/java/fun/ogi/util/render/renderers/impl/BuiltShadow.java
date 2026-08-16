package fun.ogi.util.render.renderers.impl;

import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.providers.ResourceProvider;
import fun.ogi.util.render.renderers.IRenderer;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

public record BuiltShadow(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float softness,
        float offsetX,
        float offsetY
) implements IRenderer {

    private static final ShaderProgramKey SHADOW_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("shadow"),
            VertexFormats.POSITION_COLOR, Defines.EMPTY);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float rectW = this.size.width(), rectH = this.size.height();
        float margin = 2.0f;
        float quadW = rectW + 2 * (this.softness + margin);
        float quadH = rectH + 2 * (this.softness + margin);

        ShaderProgram shader = RenderSystem.setShader(SHADOW_SHADER_KEY);
        shader.getUniform("Size").set(quadW, quadH);
        shader.getUniform("RectSize").set(rectW, rectH);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(),
                this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Offset").set(this.offsetX, this.offsetY);
        shader.getUniform("Softness").set(this.softness);

        float x0 = x - this.softness - margin;
        float y0 = y - this.softness - margin;

        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x0, y0, z).color(this.color.color1());
        builder.vertex(matrix, x0, y0 + quadH, z).color(this.color.color2());
        builder.vertex(matrix, x0 + quadW, y0 + quadH, z).color(this.color.color3());
        builder.vertex(matrix, x0 + quadW, y0, z).color(this.color.color4());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

}

