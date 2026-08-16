package fun.ogi.module.impl.list.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.ShaderUtil;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

import static fun.ogi.util.MinecraftUtil.mc;

@ModuleInformation(moduleName = "WorldTweaks", moduleDesc = "Adjust world settings like time, fog, and sky.", moduleCategory = ModuleCategory.RENDER)
public class WorldTweaks extends Module {

    public final BooleanSetting timeToggle = new BooleanSetting("Time", this, false);
    public final SliderSetting timeSetting = new SliderSetting("Time Value", this, 12000, 0, 24000, 100);

    public final BooleanSetting fogToggle = new BooleanSetting("Fog", this, false);
    public final SliderSetting distanceSetting = new SliderSetting("Fog Distance", this, 100, 20, 200, 1);
    public final SliderSetting densitySetting = new SliderSetting("Fog Density", this, 0.50, 0.0, 1.0, 0.01);
    public final BooleanSetting affectSkySetting = new BooleanSetting("Affect Sky", this, false);

    public final BooleanSetting skyToggle = new BooleanSetting("Sky", this, false);
    public final ModeSetting skyMode = new ModeSetting("Sky Mode", this, "Summer", "Summer", "Plasma", "Water","Ocean", "Off")
            .visible(() -> skyToggle.getValue());
    public final SliderSetting skySpeed = new SliderSetting("Sky Speed", this, 0.1, 0.01, 0.35, 0.01)
            .visible(() -> skyToggle.getValue() && !skyMode.is("Off"));
    public final SliderSetting skyScale = new SliderSetting("Sky Scale", this, 3.0, 0.5, 8.0, 0.1)
            .visible(() -> skyToggle.getValue() && !skyMode.is("Off"));

    public WorldTweaks() {
        addSettings(timeToggle, timeSetting, fogToggle, distanceSetting, densitySetting, affectSkySetting, skyToggle, skyMode, skySpeed, skyScale);
    }

    public boolean isTimeEnabled() {
        return isEnabled() && timeToggle.getValue();
    }

    public boolean isFogEnabled() {
        return isEnabled() && fogToggle.getValue();
    }

    public boolean isSkyEnabled() {
        return isEnabled() && skyToggle.getValue() && !skyMode.is("Off");
    }

    public boolean shouldCancelClouds() {
        return isSkyEnabled();
    }

    public float getFogDistance() {
        return (float) distanceSetting.getValue();
    }

    public float getFogDensity() {
        return (float) densitySetting.getValue();
    }

    public boolean isAffectSky() {
        return affectSkySetting.getValue();
    }

    public int getFogColor() {
        return ThemeManager.getInstance().getPrimary();
    }

    public long getForcedTime() {
        return (long) timeSetting.getValue() * 1000L;
    }

    public void renderSky(Camera camera) {
        if (!isSkyEnabled() || mc.player == null || mc.world == null || camera == null) {
            return;
        }

        ShaderProgramKey key = getShaderKey();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(key);
        if (shader == null) {
            return;
        }

        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
        float fw = Math.max(1.0f, mc.getWindow().getFramebufferWidth());
        float fh = Math.max(1.0f, mc.getWindow().getFramebufferHeight());
        int themeColor = getThemeBaseColor();
        int secondaryColor = darken(themeColor, 0.72f);
        int accentColor = overlay(themeColor, Color.WHITE.getRGB(), 0.35f);
        float modeValue = switch (skyMode.getValue()) {
            case "Plasma" -> 0.0f;
            case "Water" -> 1.0f;
            case "Ocean" -> 2.0f;
            case "Summer" -> 3.0f;
            default -> 0.0f;
        };

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(key);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        setUniform(shader, "uTime", time);
        setUniform(shader, "u_Time", time);
        setUniform(shader, "uResolution", fw, fh);
        setUniform(shader, "u_Resolution", fw, fh);
        setUniform(shader, "uColor", redf(themeColor), greenf(themeColor), bluef(themeColor));
        setUniform(shader, "u_Color", redf(themeColor), greenf(themeColor), bluef(themeColor), 1.0f);
        setUniform(shader, "u_Color2", redf(secondaryColor), greenf(secondaryColor), bluef(secondaryColor), 1.0f);
        setUniform(shader, "uAlpha", 1.0f);
        setUniform(shader, "u_Alpha", 1.0f);
        setUniform(shader, "uSpeed", (float) skySpeed.getValue());
        setUniform(shader, "u_Speed", (float) skySpeed.getValue());
        setUniform(shader, "uScale", (float) skyScale.getValue());
        setUniform(shader, "u_Scale", (float) skyScale.getValue());
        setUniform(shader, "uIntensity", 1.0f);
        setUniform(shader, "u_Intensity", 1.0f);
        setUniform(shader, "uCameraDir", 0.0f, 0.0f);
        setUniform(shader, "u_CameraDir", 0.0f, 0.0f);
        setUniform(shader, "uFov", 70.0f);
        setUniform(shader, "u_Fov", 70.0f);
        setUniform(shader, "time", time);
        setUniform(shader, "scale", (float) skyScale.getValue());
        setUniform(shader, "mode", modeValue);
        setUniform(shader, "alpha", 1.0f);
        setUniform(shader, "primaryColor", redf(themeColor), greenf(themeColor), bluef(themeColor), 1.0f);
        setUniform(shader, "secondaryColor", redf(secondaryColor), greenf(secondaryColor), bluef(secondaryColor), 1.0f);
        setUniform(shader, "accentColor", redf(accentColor), greenf(accentColor), bluef(accentColor), 1.0f);

        drawSkyCube(camera);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private ShaderProgramKey getShaderKey() {
        return switch (skyMode.getValue()) {
            case "Summer" -> ShaderUtil.customSkySummer;
            case "Plasma" -> ShaderUtil.customSkyPlasma;
            case "Water" -> ShaderUtil.customSkyWater;
            case "Ocean" -> ShaderUtil.customSkyOcean;
            default -> ShaderUtil.customSkyShader;
        };
    }

    private void drawSkyCube(Camera camera) {
        MatrixStack matrices = new MatrixStack();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        for (int face = 0; face < 6; face++) {
            matrices.push();
            if (face == 1) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            } else if (face == 2) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            } else if (face == 3) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
            } else if (face == 4) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
            } else if (face == 5) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
            }

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            buffer.vertex(matrix, -100.0f, -100.0f, -100.0f);
            buffer.vertex(matrix, -100.0f, -100.0f, 100.0f);
            buffer.vertex(matrix, 100.0f, -100.0f, 100.0f);
            buffer.vertex(matrix, 100.0f, -100.0f, -100.0f);
            matrices.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void setUniform(ShaderProgram shader, String name, float value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y, float z) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y, float z, float w) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z, w);
        }
    }

    private int getThemeBaseColor() {
        return ThemeManager.getInstance().getPrimary();
    }

    private static float redf(int color) {
        return ((color >> 16) & 0xFF) / 255.0f;
    }

    private static float greenf(int color) {
        return ((color >> 8) & 0xFF) / 255.0f;
    }

    private static float bluef(int color) {
        return (color & 0xFF) / 255.0f;
    }

    private static int darken(int color, float factor) {
        Color c = new Color(color);
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[2] = Math.max(0.0f, Math.min(1.0f, hsb[2] * factor));
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return new Color(rgb).getRGB();
    }

    private static int overlay(int base, int overlay, float alpha) {
        Color bc = new Color(base);
        Color oc = new Color(overlay);
        int r = (int) (bc.getRed() * (1 - alpha) + oc.getRed() * alpha);
        int g = (int) (bc.getGreen() * (1 - alpha) + oc.getGreen() * alpha);
        int b = (int) (bc.getBlue() * (1 - alpha) + oc.getBlue() * alpha);
        return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b)).getRGB();
    }

    @Override
    public void onDisable() {
        if (mc.player != null && mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        super.onDisable();
    }
}

