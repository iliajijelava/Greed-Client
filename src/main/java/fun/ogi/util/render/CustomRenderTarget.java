package fun.ogi.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11;

public class CustomRenderTarget extends SimpleFramebuffer {
    private boolean linear;

    public CustomRenderTarget(int width, int height, boolean useDepth) {
        super(width, height, useDepth);
        this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
    }

    public CustomRenderTarget setLinear() {
        this.linear = true;
        this.applyLinear();
        return this;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (this.linear) {
            this.applyLinear();
        }
    }

    private void applyLinear() {
        if (this.textureWidth <= 0 || this.textureHeight <= 0) return;
        RenderSystem.bindTexture(this.getColorAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.bindTexture(0);
    }

    public void setup(boolean clear) {
        if (clear) {
            this.clear();
        }
        this.beginWrite(false);
    }

    public void setup() {
        this.setup(true);
    }

    public void stop() {
        this.endWrite();
        MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
    }
}

