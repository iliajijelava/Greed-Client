package fun.ogi.module.impl.list.render;

import com.google.common.base.Suppliers;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.Priority;
import fun.ogi.events.render.EventEntityColor;
import fun.ogi.events.render.EventHud;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.mixin.WorldRendererAccessor;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.ShaderUtil;
import fun.ogi.util.render.providers.ColorProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@ModuleInformation(
   moduleName = "ShaderESP",
   moduleDesc = "Adds glow effect for selected entities",
   moduleCategory = ModuleCategory.RENDER
)
public class ShaderESP extends Module {

    public static ShaderESP INSTANCE = new ShaderESP();
    private static final float EPSILON = 0.001f;
    private static final long OUTLINE_RETRY_DELAY_MS = 3000L;
    private static final double MAX_RANGE = 256.0;
    private static final float FILL_ALPHA = 0.7f;
    private static final int FILL_MIN_ITERATIONS = 2;
    private static final float GLOW_VALUE = 0.55f;
    private static final float WIDTH_VALUE = 0.9f;

    private final ListSetting targets = new ListSetting("Targets",this,"Players","Items","Cristals","Self");


    private final BooleanSetting outline = new BooleanSetting("Outline",this, true);

    private final List<Framebuffer> bloomBuffers = new ArrayList<>();
    private int bloomWidth = -1;
    private int bloomHeight = -1;
    private boolean outlineReady;
    private boolean hasOutlineTargetsCached;
    private long nextOutlineRetryAt;

    public ShaderESP() {
        addSettings(targets, outline);
        targets.selectAll();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        outlineReady = false;
        nextOutlineRetryAt = 0L;
        tryEnsureOutlineProcessor();
    }

    @Override
    public void onDisable() {
        for (Framebuffer fb : bloomBuffers) {
            fb.delete();
        }
        bloomBuffers.clear();
        bloomWidth = -1;
        bloomHeight = -1;
        outlineReady = false;
        hasOutlineTargetsCached = false;
        nextOutlineRetryAt = 0L;
        super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.world == null || mc.worldRenderer == null) {
            outlineReady = false;
            hasOutlineTargetsCached = false;
            return;
        }
        hasOutlineTargetsCached = hasOutlineTargets();
        if (!hasOutlineTargetsCached) {
            outlineReady = false;
            return;
        }
        if (!outlineReady && System.currentTimeMillis() >= nextOutlineRetryAt) {
            tryEnsureOutlineProcessor();
        }
    }

    @Subscribe
    public void onRender2D(EventHud event) {
        if ( mc.world == null || mc.player == null || mc.worldRenderer == null) return;
        boolean hasGlow = GLOW_VALUE > EPSILON;
        boolean hasOutline = outline.getValue();
        if (!hasGlow && !hasOutline) return;
        if (!hasOutlineTargetsCached) return;
        if (!tryEnsureOutlineProcessor()) return;

        Framebuffer outlineBuffer = getOutlineSourceFramebuffer();
        if (outlineBuffer == null || outlineBuffer.getColorAttachment() == 0) return;

        Framebuffer mainBuffer = mc.getFramebuffer();

        int iterations = Math.max(1, Math.min(8, (int) Math.ceil(WIDTH_VALUE * 1.25f)));
        int fillTexture = 0;
        int blurredTexture = hasGlow
                ? runKawaseBloom(outlineBuffer.getColorAttachment(), iterations)
                : fillTexture;
        int color = getOutlineColor();

        mainBuffer.beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(true, true, true, false);

        if (hasOutline) {
            ShaderProgram outlineShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtil.shaderEspOutline);
            if (outlineShader != null) {
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SrcFactor.SRC_ALPHA,
                        GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SrcFactor.ZERO,
                        GlStateManager.DstFactor.ONE
                );
                RenderSystem.setShader(ShaderUtil.shaderEspOutline);
                RenderSystem.setShaderTexture(0, outlineBuffer.getColorAttachment());
                setUniform(outlineShader, "color", ColorProvider.red(color), ColorProvider.green(color), ColorProvider.blue(color));
                setUniform(outlineShader, "alpha", 1.0f);
                drawFullscreenQuad();
            }
        }

        if (hasGlow) {
            ShaderProgram glowShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtil.shaderEspGlow);
            if (glowShader != null) {
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SrcFactor.SRC_ALPHA,
                        GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SrcFactor.ZERO,
                        GlStateManager.DstFactor.ONE
                );
                RenderSystem.setShader(ShaderUtil.shaderEspGlow);
                RenderSystem.setShaderTexture(0, blurredTexture);
                RenderSystem.setShaderTexture(1, outlineBuffer.getColorAttachment());
                setUniform(glowShader, "color", ColorProvider.red(color), ColorProvider.green(color), ColorProvider.blue(color));
                setUniform(glowShader, "color2", ColorProvider.red(color), ColorProvider.green(color), ColorProvider.blue(color));
                setUniform(glowShader, "exposure", 0.015f + GLOW_VALUE * 0.065f);
                setUniform(glowShader, "time", (System.currentTimeMillis() % 100000L) / 1000.0f);
                setUniform(glowShader, "animate", 1.0f);
                drawFullscreenQuadWithDepthTest(mainBuffer, outlineBuffer);
            }
        }

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.setShaderTexture(1, 0);
        mainBuffer.beginWrite(true);
    }

    private void drawFullscreenQuadWithDepthTest(Framebuffer mainBuffer, Framebuffer outlineBuffer) {
        drawFullscreenQuad();
    }

    private boolean tryEnsureOutlineProcessor() {
        if (mc.world == null || mc.worldRenderer == null) {
            outlineReady = false;
            return false;
        }
        Framebuffer outlines = getOutlineSourceFramebuffer();
        if (outlines != null && outlines.getColorAttachment() != 0) {
            outlineReady = true;
            return true;
        }
        if (outlineReady) {
            outlineReady = false;
        }
        if (System.currentTimeMillis() < nextOutlineRetryAt) {
            return false;
        }
        try {
            mc.worldRenderer.loadEntityOutlinePostProcessor();
            outlines = getOutlineSourceFramebuffer();
            outlineReady = outlines != null && outlines.getColorAttachment() != 0;
            if (!outlineReady) {
                nextOutlineRetryAt = System.currentTimeMillis() + OUTLINE_RETRY_DELAY_MS;
            }
            return outlineReady;
        } catch (Throwable ignored) {
            outlineReady = false;
            nextOutlineRetryAt = System.currentTimeMillis() + OUTLINE_RETRY_DELAY_MS;
            return false;
        }
    }

    private Framebuffer getOutlineSourceFramebuffer() {
        if (mc.worldRenderer instanceof WorldRendererAccessor accessor) {
            Framebuffer raw = accessor.cheap$getEntityOutlineFramebufferRaw();
            if (raw != null && raw.getColorAttachment() != 0) {
                return raw;
            }
        }
        return mc.worldRenderer.getEntityOutlinesFramebuffer();
    }

    public boolean shouldOutline(Entity entity) {
        if (entity == null || mc.player == null || mc.world == null) return false;
        if (!entity.isAlive()) return false;
        if (entity.isRemoved()) return false;
        if (entity == mc.player && !targets.isSelected("Self")) return false;
        if (entity.squaredDistanceTo(mc.player) > MAX_RANGE * MAX_RANGE) return false;

        if (entity instanceof PlayerEntity) {
            return targets.isSelected("Players");
        }
        if (entity instanceof EndCrystalEntity) {
            return targets.isSelected("Crystals");
        }
        if (entity instanceof ItemEntity) {
            return targets.isSelected("Items");
        }
        return false;
    }

    private boolean hasOutlineTargets() {
        if (mc.world == null || mc.player == null) {
            return false;
        }
        for (Entity entity : mc.world.getEntities()) {
            if (shouldOutline(entity)) {
                return true;
            }
        }
        return false;
    }

    public int getOutlineColor() {
        return ThemeManager.getInstance().getPrimary() & 0xFFFFFF;
    }

    private int runKawaseBloom(int sourceTexture, int iterations) {
        ensureBloomBuffers(iterations);
        if (bloomBuffers.isEmpty()) {
            return sourceTexture;
        }

        int currentTexture = sourceTexture;
        ShaderProgram downShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtil.shaderHandsKawaseDown);
        ShaderProgram upShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtil.shaderHandsKawaseUp);
        if (downShader == null || upShader == null) {
            return currentTexture;
        }

        for (int i = 0; i < iterations; i++) {
            Framebuffer dst = bloomBuffers.get(i);
            dst.setClearColor(0f, 0f, 0f, 0f);
            dst.clear();
            dst.beginWrite(true);

            RenderSystem.setShader(ShaderUtil.shaderHandsKawaseDown);
            RenderSystem.setShaderTexture(0, currentTexture);
            setHandsKawaseUniforms(downShader, dst.textureWidth, dst.textureHeight, 1.0f + i);
            drawFullscreenQuad();
            currentTexture = dst.getColorAttachment();
        }

        for (int i = iterations - 1; i >= 1; i--) {
            Framebuffer dst = bloomBuffers.get(i - 1);
            dst.setClearColor(0f, 0f, 0f, 0f);
            dst.clear();
            dst.beginWrite(true);

            RenderSystem.setShader(ShaderUtil.shaderHandsKawaseUp);
            RenderSystem.setShaderTexture(0, currentTexture);
            setHandsKawaseUniforms(upShader, dst.textureWidth, dst.textureHeight, 1.0f + i);
            setUniform(upShader, "color", 1.0f, 1.0f, 1.0f);
            drawFullscreenQuad();
            currentTexture = dst.getColorAttachment();
        }

        mc.getFramebuffer().beginWrite(true);
        return currentTexture;
    }

    private void ensureBloomBuffers(int iterations) {
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();

        if (bloomWidth != w || bloomHeight != h) {
            for (Framebuffer fb : bloomBuffers) {
                fb.delete();
            }
            bloomBuffers.clear();
            bloomWidth = w;
            bloomHeight = h;
        }

        while (bloomBuffers.size() > iterations) {
            int last = bloomBuffers.size() - 1;
            bloomBuffers.get(last).delete();
            bloomBuffers.remove(last);
        }

        for (int i = 0; i < iterations; i++) {
            int tw = Math.max(2, w >> (i + 1));
            int th = Math.max(2, h >> (i + 1));
            if (i >= bloomBuffers.size()) {
                Framebuffer fb = new SimpleFramebuffer(tw, th, false);
                setLinearFiltering(fb);
                bloomBuffers.add(fb);
                continue;
            }

            Framebuffer fb = bloomBuffers.get(i);
            if (fb.textureWidth != tw || fb.textureHeight != th) {
                fb.delete();
                fb = new SimpleFramebuffer(tw, th, false);
                setLinearFiltering(fb);
                bloomBuffers.set(i, fb);
            }
        }
    }

    private void setLinearFiltering(Framebuffer fb) {
        RenderSystem.bindTexture(fb.getColorAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.bindTexture(0);
    }

    private void setUniform(ShaderProgram shader, String name, float value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y, float z) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y, z);
    }

    private void setHandsKawaseUniforms(ShaderProgram shader, int texWidth, int texHeight, float offset) {
        setUniform(shader, "uSize", Math.max(1, texWidth), Math.max(1, texHeight));
        setUniform(shader, "uOffset", offset, offset);
        setUniform(shader, "uHalfPixel", 0.5f / Math.max(1, texWidth), 0.5f / Math.max(1, texHeight));
    }

    private void drawFullscreenQuad() {
        float sw = Math.max(mc.getWindow().getScaledWidth(), 1);
        float sh = Math.max(mc.getWindow().getScaledHeight(), 1);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(0, 0, 0).texture(0, 1).color(1f, 1f, 1f, 1f);
        buffer.vertex(0, sh, 0).texture(0, 0).color(1f, 1f, 1f, 1f);
        buffer.vertex(sw, sh, 0).texture(1, 0).color(1f, 1f, 1f, 1f);
        buffer.vertex(sw, 0, 0).texture(1, 1).color(1f, 1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}

