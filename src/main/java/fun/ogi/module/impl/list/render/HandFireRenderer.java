package fun.ogi.module.impl.list.render;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.util.GlProgram;
import fun.ogi.util.render.CustomRenderTarget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;








public class HandFireRenderer {

    private static final HandFireRenderer INSTANCE = new HandFireRenderer();
    private static final float[] WHITE = {1f, 1f, 1f};

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private CustomRenderTarget sceneBefore;   
    private CustomRenderTarget sceneAfter;    
    private CustomRenderTarget maskTarget;    
    private CustomRenderTarget trailA;        
    private CustomRenderTarget trailB;        
    private CustomRenderTarget blur1;
    private CustomRenderTarget blur2;

    private GlProgram blitProgram;
    private GlProgram maskDiffProgram;
    private GlProgram trailProgram;
    private GlProgram kawaseDownProgram;
    private GlProgram kawaseUpProgram;
    private GlProgram fireProgram;
    private GlProgram prettyFireProgram;
    private GlProgram blurMixProgram;

    private boolean enabled = false;
    private boolean beforeCaptured = false;
    private boolean afterCaptured = false;

    
    private long lastMs = 0L;
    private float smoothDt = 1f / 60f;
    private float smoothActivity = 0f;
    private float slash = 0f;
    private float slashDir = 1f;
    private boolean wasSwinging = false;
    private float prevSwingProgress = 0f;
    private final long startNanos = System.nanoTime();

    
    private float prevYaw = 0f;
    private float prevPitch = 0f;
    private boolean hasPrevCam = false;

    private HandFireRenderer() {
    }

    public static HandFireRenderer getInstance() {
        return INSTANCE;
    }

    public static void initializeShaders() {
        INSTANCE.blitProgram = new GlProgram(Identifier.of("cheap", "hands/hands_blit"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.maskDiffProgram = new GlProgram(Identifier.of("cheap", "handstrail/mask_diff"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.trailProgram = new GlProgram(Identifier.of("cheap", "handstrail/hand_trail"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.kawaseDownProgram = new GlProgram(Identifier.of("cheap", "hands/hands_kawase_down"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.kawaseUpProgram = new GlProgram(Identifier.of("cheap", "hands/hands_kawase_up"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.fireProgram = new GlProgram(Identifier.of("cheap", "handstrail/hand_fire"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.prettyFireProgram = new GlProgram(Identifier.of("cheap", "handstrail/hands_fire_pretty"), VertexFormats.POSITION_TEXTURE_COLOR);
        INSTANCE.blurMixProgram = new GlProgram(Identifier.of("cheap", "hands/hands_blur_mix"), VertexFormats.POSITION_TEXTURE_COLOR);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            invalidateState();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    

    
    
    
    private void ensureProgramsLoaded() {
        ShaderLoader loader = mc.getShaderLoader();
        if (loader == null) return;
        if (blitProgram == null) {
            initializeShaders();
        }
        GlProgram.loadAndSetupPrograms();
    }

    

     
    private boolean isWindowReady() {
        if (!enabled) return false;
        if (!mc.isWindowFocused()) return false;
        return mc.getWindow().getFramebufferWidth() > 0 && mc.getWindow().getFramebufferHeight() > 0;
    }

    public void captureSceneBeforeHands() {
        ensureProgramsLoaded();
        if (!isWindowReady() || blitProgram == null || !blitProgram.isLoaded()) return;
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        ensureTargets(w, h);

        begin2D();
        copyTexture(mc.getFramebuffer().getColorAttachment(), sceneBefore);
        end2D();
        beforeCaptured = true;
        afterCaptured = false;
    }

    public void captureSceneAfterHands() {
        if (!isWindowReady() || !beforeCaptured || blitProgram == null || !blitProgram.isLoaded()) return;
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        ensureTargets(w, h);

        begin2D();
        copyTexture(mc.getFramebuffer().getColorAttachment(), sceneAfter);
        end2D();
        afterCaptured = true;
    }

    

    public void renderFireEffect() {
        ensureProgramsLoaded();
        if (!enabled || !afterCaptured) {
            beforeCaptured = false;
            afterCaptured = false;
            return;
        }
        HandFire m = HandFire.INSTANCE;
        boolean prettyMode = m.mode.is("Pretty");
        GlProgram compositeProgram = prettyMode ? prettyFireProgram : fireProgram;
        if (trailProgram == null || !trailProgram.isLoaded() || compositeProgram == null
                || !compositeProgram.isLoaded() || maskDiffProgram == null || !maskDiffProgram.isLoaded()) {
            beforeCaptured = false;
            afterCaptured = false;
            return;
        }

        
        
        
        if (!isWindowReady()) {
            clearTrails();
            hasPrevCam = false;
            lastMs = 0L;
            beforeCaptured = false;
            afterCaptured = false;
            return;
        }

        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();

        
        long now = System.currentTimeMillis();
        float rawDt = lastMs > 0L ? (now - lastMs) / 1000f : 1f / 60f;
        lastMs = now;
        if (rawDt <= 0f || rawDt > 0.05f) rawDt = smoothDt;
        rawDt = Math.max(1f / 144f, Math.min(1f / 30f, rawDt));
        smoothDt += (rawDt - smoothDt) * 0.1f;
        float dt = smoothDt;
        float time = (System.nanoTime() - startNanos) / 1_000_000_000.0f;

        boolean swinging = mc.player != null && mc.player.handSwinging;
        float swingProgress = mc.player != null ? mc.player.handSwingProgress : 0f;
        boolean restartedSwing = swinging && (swingProgress + 0.08f < prevSwingProgress || (!wasSwinging && swingProgress > 0.02f));
        if (restartedSwing) {
            slash = 1f;
            slashDir = -slashDir;
        }
        wasSwinging = swinging;
        prevSwingProgress = swingProgress;
        if (swinging) {
            
            
            slash = Math.max(slash, 1.0f - Math.min(1.0f, swingProgress) * 0.72f);
        }
        slash = Math.max(0f, slash - dt * 2.2f);

        float motion = 0f;
        if (mc.player != null) {
            motion = (float) Math.min(1.0, mc.player.getVelocity().horizontalLength() * 2.2);
        }
        float swingActivity = swinging && mc.player != null ? 1f - Math.min(1f, swingProgress) : 0f;
        float useActivity = mc.player != null && mc.player.isUsingItem() ? 0.55f + Math.min(0.45f, mc.player.getItemUseTime() * 0.06f) : 0f;
        float activityTarget = Math.max(Math.max(swingActivity, useActivity), motion);
        smoothActivity += (activityTarget - smoothActivity) * (1f - (float) Math.exp(-dt * 6.0));
        float activity = smoothActivity;

        float[] glow = m.glowColor();

        
        
        
        float camShiftX = 0f;
        float camShiftY = 0f;
        if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
            float yaw = mc.gameRenderer.getCamera().getYaw();
            float pitch = mc.gameRenderer.getCamera().getPitch();
            if (hasPrevCam) {
                float dYaw = wrapDegrees(yaw - prevYaw);
                float dPitch = pitch - prevPitch;
                double fovDeg = mc.options.getFov().getValue();
                float fovRad = (float) Math.toRadians(Math.max(1.0, fovDeg));
                float tanHalf = (float) Math.tan(fovRad * 0.5f);
                float aspect = h > 0 ? (float) w / (float) h : 1f;
                
                camShiftX = (float) Math.toRadians(dYaw) / (2f * tanHalf * aspect);
                camShiftY = -(float) Math.toRadians(dPitch) / (2f * tanHalf);
                
                camShiftX = Math.max(-0.25f, Math.min(0.25f, camShiftX));
                camShiftY = Math.max(-0.25f, Math.min(0.25f, camShiftY));
            }
            prevYaw = yaw;
            prevPitch = pitch;
            hasPrevCam = true;
        }

        begin2D();

        
        maskTarget.setup(true);
        RenderSystem.disableBlend();
        maskDiffProgram.use();
        RenderSystem.setShaderTexture(0, sceneBefore.getColorAttachment());
        RenderSystem.setShaderTexture(1, sceneAfter.getColorAttachment());
        drawFullScreenQuad();
        maskTarget.stop();

        
        trailB.setup(true);
        RenderSystem.disableBlend();
        trailProgram.use();
        RenderSystem.setShaderTexture(0, trailA.getColorAttachment());
        RenderSystem.setShaderTexture(1, sceneAfter.getColorAttachment());
        RenderSystem.setShaderTexture(2, maskTarget.getColorAttachment());
        setUniform(trailProgram, "texSize", (float) w, (float) h);
        setUniform(trailProgram, "time", time);
        setUniform(trailProgram, "intensity", m.intensity.getFloatValue());
        setUniform(trailProgram, "speed", m.speed.getFloatValue());
        setUniform(trailProgram, "length", m.length.getFloatValue());
        setUniform(trailProgram, "trailSoftness", m.trailSoftness.getFloatValue());
        setUniform(trailProgram, "trailBlur", m.trailBlur.getFloatValue());
        setUniform(trailProgram, "smoke", m.smoke.getFloatValue());
        setUniform(trailProgram, "activity", activity);
        setUniform(trailProgram, "trailFade", m.trailFade.getFloatValue());
        setUniform(trailProgram, "slash", slash);
        setUniform(trailProgram, "slashDir", slashDir);
        float swingHand = 0.0f;
        if (mc.player != null) {
            Hand activeSwingHand = swinging ? Hand.MAIN_HAND
                    : (mc.player.isUsingItem() ? mc.player.getActiveHand() : Hand.MAIN_HAND);
            if (activeSwingHand == Hand.MAIN_HAND) {
                swingHand = mc.player.getMainArm() == Arm.LEFT ? -1.0f : 1.0f;
            } else if (activeSwingHand == Hand.OFF_HAND) {
                swingHand = mc.player.getMainArm() == Arm.LEFT ? 1.0f : -1.0f;
            }
        }
        setUniform(trailProgram, "swingHand", swingHand);
        setUniform(trailProgram, "camShift", camShiftX, camShiftY);
        setUniform(trailProgram, "glowColor", glow[0], glow[1], glow[2], 1f);
        drawFullScreenQuad();
        trailB.stop();

        
        int smokeTex;
        if (prettyMode) {
            smokeTex = buildBlur(w, h, maskTarget.getColorAttachment(), m.prettyGlow.getFloatValue());
        } else {
            smokeTex = buildBlur(w, h, trailB.getColorAttachment(), m.trailBlur.getFloatValue());
        }

        
        mc.getFramebuffer().beginWrite(false);
        if (prettyMode) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 1, 0, 1);
            prettyFireProgram.use();
            RenderSystem.setShaderTexture(0, smokeTex);
            RenderSystem.setShaderTexture(1, maskTarget.getColorAttachment());
            setUniform(prettyFireProgram, "color", glow[0], glow[1], glow[2]);
            setUniform(prettyFireProgram, "color2", Math.min(1f, glow[0] + 0.25f), Math.max(0f, glow[1] * 0.55f), Math.max(0f, glow[2] * 0.45f));
            setUniform(prettyFireProgram, "time", time);
            setUniform(prettyFireProgram, "height", m.prettyHeight.getFloatValue());
            setUniform(prettyFireProgram, "speed", m.speed.getFloatValue());
            setUniform(prettyFireProgram, "intensity", m.intensity.getFloatValue());
            setUniform(prettyFireProgram, "windStrength", m.prettyWind.getFloatValue());
            setUniform(prettyFireProgram, "waveStrength", m.prettyWave.getFloatValue());
            setUniform(prettyFireProgram, "camOffset", camShiftX, camShiftY);
            drawFullScreenQuad();
            RenderSystem.disableBlend();
        } else {
            RenderSystem.disableBlend();
            fireProgram.use();
            RenderSystem.setShaderTexture(0, sceneAfter.getColorAttachment());
            RenderSystem.setShaderTexture(1, smokeTex);
            RenderSystem.setShaderTexture(2, maskTarget.getColorAttachment());
            setUniform(fireProgram, "texSize", (float) w, (float) h);
            setUniform(fireProgram, "time", time);
            setUniform(fireProgram, "intensity", m.intensity.getFloatValue());
            setUniform(fireProgram, "handSoftness", m.handSoftness.getFloatValue());
            setUniform(fireProgram, "handBlur", m.handBlur.getFloatValue());
            setUniform(fireProgram, "smoke", m.smoke.getFloatValue());
            setUniform(fireProgram, "activity", activity);
            setUniform(fireProgram, "glowColor", glow[0], glow[1], glow[2], 1f);
            drawFullScreenQuad();
        }

        
        CustomRenderTarget tmp = trailA;
        trailA = trailB;
        trailB = tmp;

        end2D();
        beforeCaptured = false;
        afterCaptured = false;
    }

    

    private int buildBlur(int w, int h, int srcTex, float radius) {
        ensureBlurTargets(w, h);
        float o = Math.max(1f, 1f + radius);
        kawasePass(kawaseDownProgram, blur1, srcTex, o, w, h, null);
        kawasePass(kawaseDownProgram, blur2, blur1.getColorAttachment(), o * 2f, w, h, null);
        kawasePass(kawaseUpProgram, blur1, blur2.getColorAttachment(), o * 2f, w, h, WHITE);
        kawasePass(kawaseUpProgram, blur2, blur1.getColorAttachment(), o, w, h, WHITE);
        return blur2.getColorAttachment();
    }

    private void kawasePass(GlProgram program, CustomRenderTarget dst, int srcTex, float offset,
                            int width, int height, float[] color) {
        dst.setup(true);
        RenderSystem.disableBlend();
        program.use();
        RenderSystem.setShaderTexture(0, srcTex);
        setUniform(program, "uOffset", offset, offset);
        setUniform(program, "uHalfPixel", 0.5f / width, 0.5f / height);
        setUniform(program, "uSize", (float) width, (float) height);
        if (color != null) {
            setUniform(program, "color", color[0], color[1], color[2]);
        }
        drawFullScreenQuad();
        dst.stop();
    }

    

    private Matrix4f savedProjection;
    private ProjectionType savedProjectionType;

    private void begin2D() {
        savedProjection = RenderSystem.getProjectionMatrix();
        savedProjectionType = RenderSystem.getProjectionType();
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.setProjectionMatrix(new Matrix4f(), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
    }

    private void end2D() {
        RenderSystem.setProjectionMatrix(savedProjection, savedProjectionType);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void copyTexture(int srcTex, CustomRenderTarget dst) {
        dst.setup(true);
        RenderSystem.disableBlend();
        blitProgram.use();
        RenderSystem.setShaderTexture(0, srcTex);
        setUniform(blitProgram, "colorMul", 1f, 1f, 1f, 1f);
        drawFullScreenQuad();
        dst.stop();
    }

    private void ensureTargets(int w, int h) {
        if (sceneBefore == null) {
            sceneBefore = new CustomRenderTarget(w, h, false);
            sceneAfter = new CustomRenderTarget(w, h, false);
            sceneAfter.setLinear();
            maskTarget = new CustomRenderTarget(w, h, false);
            trailA = new CustomRenderTarget(w, h, false);
            trailA.setLinear();
            trailB = new CustomRenderTarget(w, h, false);
            trailB.setLinear();
        } else if (sceneBefore.textureWidth != w || sceneBefore.textureHeight != h) {
            sceneBefore.resize(w, h);
            sceneAfter.resize(w, h);
            maskTarget.resize(w, h);
            trailA.resize(w, h);
            trailB.resize(w, h);
        }
    }

    private void ensureBlurTargets(int w, int h) {
        if (blur1 == null) {
            blur1 = new CustomRenderTarget(w, h, false);
            blur1.setLinear();
            blur2 = new CustomRenderTarget(w, h, false);
            blur2.setLinear();
        } else if (blur1.textureWidth != w || blur1.textureHeight != h) {
            blur1.resize(w, h);
            blur2.resize(w, h);
        }
    }

     
    private void clearTrails() {
        if (trailA != null) { trailA.setup(true); trailA.stop(); }
        if (trailB != null) { trailB.setup(true); trailB.stop(); }
    }

    private void drawFullScreenQuad() {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = new Matrix4f();
        builder.vertex(matrix, -1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
        builder.vertex(matrix, -1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
        builder.vertex(matrix, 1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
        builder.vertex(matrix, 1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

     
    private static float wrapDegrees(float deg) {
        deg %= 360f;
        if (deg >= 180f) deg -= 360f;
        if (deg < -180f) deg += 360f;
        return deg;
    }

    private void setUniform(GlProgram program, String name, float... values) {
        GlUniform uniform = program.findUniform(name);
        if (uniform != null) {
            if (values.length == 1) uniform.set(values[0]);
            else if (values.length == 2) uniform.set(values[0], values[1]);
            else if (values.length == 3) uniform.set(values[0], values[1], values[2]);
            else if (values.length == 4) uniform.set(values[0], values[1], values[2], values[3]);
        }
    }

    public void invalidateState() {
        if (sceneBefore != null) { sceneBefore.delete(); sceneBefore = null; }
        if (sceneAfter != null) { sceneAfter.delete(); sceneAfter = null; }
        if (maskTarget != null) { maskTarget.delete(); maskTarget = null; }
        if (trailA != null) { trailA.delete(); trailA = null; }
        if (trailB != null) { trailB.delete(); trailB = null; }
        if (blur1 != null) { blur1.delete(); blur1 = null; }
        if (blur2 != null) { blur2.delete(); blur2 = null; }
        beforeCaptured = false;
        afterCaptured = false;
        lastMs = 0L;
        smoothDt = 1f / 60f;
        smoothActivity = 0f;
        slash = 0f;
        slashDir = 1f;
        wasSwinging = false;
        prevSwingProgress = 0f;
    }
}

