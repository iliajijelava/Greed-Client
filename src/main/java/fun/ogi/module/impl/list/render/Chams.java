package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.mixin.LivingEntityRendererAccessor;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.NumberSetting;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

import fun.ogi.util.ShaderUtil;

@ModuleInformation(moduleName = "Chams", moduleDesc = "Player model chams with waves/glow/outline", moduleCategory = ModuleCategory.RENDER)
public class Chams extends Module {
    public static final String TARGET_PLAYERS = "Players";
    public static final String TARGET_FRIENDS = "Friends";
    public static final String TARGET_SELF = "Self";

    private static final int DEFAULT_FILL_ALPHA = 130;
    private static final float DEFAULT_LINE_WIDTH = 0.5f;
    private static final float CLIENT_FILL_SATURATION = 1.18f;
    private static final float CLIENT_FILL_BRIGHTNESS = 1.12f;
    private static final float CLIENT_OUTLINE_SATURATION = 1.12f;
    private static final float CLIENT_OUTLINE_BRIGHTNESS = 1.08f;
    private static final float MIN_PULSE_ALPHA = 0.65f;
    private static final float PULSE_SWING = 0.35f;
    private static final int FRIEND_FILL_COLOR = new Color(85, 255, 85, 60).getRGB();
    private static final int FRIEND_OUTLINE_COLOR = new Color(100, 255, 100, 255).getRGB();

    private final ListSetting rendering = new ListSetting("Targets", this, TARGET_PLAYERS, TARGET_FRIENDS, TARGET_SELF);
    private final BooleanSetting waves = new BooleanSetting("Waves", this, true);
    private final NumberSetting waveSpeedX = new NumberSetting("Wave Speed X", this, 0.22, 0.0, 1.5, 0.01);
    private final NumberSetting waveSpeedY = new NumberSetting("Wave Speed Y", this, 0.15, 0.0, 1.5, 0.01);
    private final NumberSetting waveScale = new NumberSetting("Wave Scale", this, 1.35, 0.2, 4.0, 0.05);
    private final NumberSetting waveDensity = new NumberSetting("Wave Density", this, 1.15, 0.5, 3.0, 0.05);
    private final NumberSetting waveGlow = new NumberSetting("Wave Glow", this, 1.0, 0.2, 3.0, 0.05);
    private final BooleanSetting glow = new BooleanSetting("Glow", this, true);
    private final NumberSetting glowIntensity = new NumberSetting("Glow Intensity", this, 2.0, 1.0, 5.0, 0.1);
    private final NumberSetting glowLayers = new NumberSetting("Glow Layers", this, 3.0, 1.0, 6.0, 1.0);
    private final BooleanSetting pulse = new BooleanSetting("Pulse", this, false);
    private final NumberSetting pulseSpeed = new NumberSetting("Pulse Speed", this, 2.0, 0.5, 5.0, 0.1);
    private final BooleanSetting hideOriginal = new BooleanSetting("Hide Original", this, false);
    private final BooleanSetting hideItemsAndCape = new BooleanSetting("Hide Items & Cape", this, false);

    private final long startTime = System.currentTimeMillis();

    public Chams() {
        addSettings(rendering, waves, waveSpeedX, waveSpeedY, waveScale, waveDensity, waveGlow,
                glow, glowIntensity, glowLayers, pulse, pulseSpeed, hideOriginal, hideItemsAndCape);
        rendering.select(TARGET_PLAYERS);
        rendering.select(TARGET_FRIENDS);
    }

    @Subscribe
    public void onRender3D(EventWorldRenderer event) {
        if (!isEnabled() || mc.world == null || mc.player == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!affects(player)) continue;
            if (player == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) continue;
            renderManualPlayer(event, player);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    private void renderManualPlayer(EventWorldRenderer event, PlayerEntity player) {
        if (!(player instanceof AbstractClientPlayerEntity clientPlayer)) return;

        EntityRenderer<?, ?> rawRenderer = mc.getEntityRenderDispatcher().getRenderer(player);
        if (!(rawRenderer instanceof PlayerEntityRenderer renderer)) return;

        float tickDelta = event.getRenderTickCounter().getTickDelta(false);
        PlayerEntityRenderState state = renderer.createRenderState();
        renderer.updateRenderState(clientPlayer, state, tickDelta);
        PlayerEntityModel model = renderer.getModel();
        model.setAngles(state);

        MatrixStack matrices = event.getMatrices();
        matrices.push();
        setupModelMatrix(matrices, state, renderer, player, tickDelta, event.getCamera().getPos());

        int fillColor = resolveFillColor(player);
        int outlineColor = resolveOutlineColor(player);
        renderFillModel(matrices, model, 0.0f, fillColor);
        renderOutlineModel(matrices, model, 0.0f, outlineColor);

        matrices.pop();
    }

    private void setupModelMatrix(MatrixStack matrices, PlayerEntityRenderState state, PlayerEntityRenderer renderer, PlayerEntity player, float tickDelta, Vec3d cam) {
        Vec3d pos = player.getLerpedPos(tickDelta);
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);

        float baseScale = state.baseScale;
        matrices.scale(baseScale, baseScale, baseScale);
        LivingEntityRendererAccessor accessor = (LivingEntityRendererAccessor) renderer;
        accessor.invokeSetupTransforms(state, matrices, state.bodyYaw, baseScale);
        matrices.scale(-1.0f, -1.0f, 1.0f);
        accessor.invokeScale(state, matrices);
        matrices.translate(0.0f, -1.501f, 0.0f);
    }

    private void renderFillModel(MatrixStack matrices, BipedEntityModel<?> model, float expand, int color) {
        if (waves.getValue()) {
            ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtil.chamsFill);
            if (shader == null) return;

            RenderSystem.setShader(ShaderUtil.chamsFill);
            float time = (System.currentTimeMillis() - startTime) / 1000.0f;
            setUniform(shader, "time", time);
            setUniform(shader, "speedX", (float) waveSpeedX.getValue());
            setUniform(shader, "speedY", (float) waveSpeedY.getValue());
            setUniform(shader, "scale", (float) waveScale.getValue());
            setUniform(shader, "density", (float) waveDensity.getValue());
            setUniform(shader, "glowStrength", (float) waveGlow.getValue());

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            var root = model.getRootPart();
            renderFillPart(matrices, buffer, root, model.head, -4, -8, -4, 8, 8, 8, expand, color);
            renderFillPart(matrices, buffer, root, model.body, -4, 0, -2, 8, 12, 4, expand, color);
            renderFillPart(matrices, buffer, root, model.rightArm, -3, -2, -2, 4, 12, 4, expand, color);
            renderFillPart(matrices, buffer, root, model.leftArm, -1, -2, -2, 4, 12, 4, expand, color);
            renderFillPart(matrices, buffer, root, model.rightLeg, -2, 0, -2, 4, 12, 4, expand, color);
            renderFillPart(matrices, buffer, root, model.leftLeg, -2, 0, -2, 4, 12, 4, expand, color);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            var root = model.getRootPart();
            renderSolidFillPart(matrices, buffer, root, model.head, -4, -8, -4, 8, 8, 8, expand, color);
            renderSolidFillPart(matrices, buffer, root, model.body, -4, 0, -2, 8, 12, 4, expand, color);
            renderSolidFillPart(matrices, buffer, root, model.rightArm, -3, -2, -2, 4, 12, 4, expand, color);
            renderSolidFillPart(matrices, buffer, root, model.leftArm, -1, -2, -2, 4, 12, 4, expand, color);
            renderSolidFillPart(matrices, buffer, root, model.rightLeg, -2, 0, -2, 4, 12, 4, expand, color);
            renderSolidFillPart(matrices, buffer, root, model.leftLeg, -2, 0, -2, 4, 12, 4, expand, color);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
    }

    private void renderFillPart(MatrixStack baseStack, BufferBuilder buffer, net.minecraft.client.model.ModelPart root, net.minecraft.client.model.ModelPart part,
                                float offX, float offY, float offZ, float width, float height, float depth, float expand, int color) {
        baseStack.push();
        root.rotate(baseStack);
        part.rotate(baseStack);

        Matrix4f matrix = baseStack.peek().getPositionMatrix();
        float scale = 1f / 16f;
        float expandScale = expand * scale;

        float minX = offX * scale - expandScale;
        float minY = offY * scale - expandScale;
        float minZ = offZ * scale - expandScale;
        float maxX = (offX + width) * scale + expandScale;
        float maxY = (offY + height) * scale + expandScale;
        float maxZ = (offZ + depth) * scale + expandScale;

        addQuad(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        addQuad(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, color);
        addQuad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
        addQuad(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, color);
        addQuad(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, color);
        addQuad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);

        baseStack.pop();
    }

    private void renderSolidFillPart(MatrixStack baseStack, BufferBuilder buffer, net.minecraft.client.model.ModelPart root, net.minecraft.client.model.ModelPart part,
                                     float offX, float offY, float offZ, float width, float height, float depth, float expand, int color) {
        baseStack.push();
        root.rotate(baseStack);
        part.rotate(baseStack);

        Matrix4f matrix = baseStack.peek().getPositionMatrix();
        float scale = 1f / 16f;
        float expandScale = expand * scale;

        float minX = offX * scale - expandScale;
        float minY = offY * scale - expandScale;
        float minZ = offZ * scale - expandScale;
        float maxX = (offX + width) * scale + expandScale;
        float maxY = (offY + height) * scale + expandScale;
        float maxZ = (offZ + depth) * scale + expandScale;

        addSolidQuad(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        addSolidQuad(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, color);
        addSolidQuad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
        addSolidQuad(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, color);
        addSolidQuad(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, color);
        addSolidQuad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);

        baseStack.pop();
    }

    private void addQuad(BufferBuilder buffer, Matrix4f matrix,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         int color) {
        int r = red(color);
        int g = green(color);
        int b = blue(color);
        int a = alpha(color);

        float u1 = waveU(x1, y1, z1);
        float v1 = waveV(x1, y1, z1);
        float u2 = waveU(x2, y2, z2);
        float v2 = waveV(x2, y2, z2);
        float u3 = waveU(x3, y3, z3);
        float v3 = waveV(x3, y3, z3);
        float u4 = waveU(x4, y4, z4);
        float v4 = waveV(x4, y4, z4);

        buffer.vertex(matrix, x1, y1, z1).texture(u1, v1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).texture(u2, v2).color(r, g, b, a);
        buffer.vertex(matrix, x3, y3, z3).texture(u3, v3).color(r, g, b, a);
        buffer.vertex(matrix, x4, y4, z4).texture(u4, v4).color(r, g, b, a);
    }

    private void addSolidQuad(BufferBuilder buffer, Matrix4f matrix,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float x4, float y4, float z4,
                              int color) {
        int r = red(color);
        int g = green(color);
        int b = blue(color);
        int a = alpha(color);

        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a);
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, a);
    }

    private float waveU(float x, float y, float z) {
        return x * 1.15f + z * 0.72f;
    }

    private float waveV(float x, float y, float z) {
        return y * 1.05f - z * 0.38f + x * 0.18f;
    }

    private void renderOutlineModel(MatrixStack matrices, BipedEntityModel<?> model, float expand, int color) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        RenderSystem.lineWidth(DEFAULT_LINE_WIDTH);

        if (glow.getValue()) {
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
            int layers = Math.max(1, Math.round(glowLayers.getFloatValue()));
            float intensity = Math.max(1.0f, glowIntensity.getFloatValue());
            for (int index = layers; index >= 1; index--) {
                float layerExpand = expand + index * 0.5f * intensity;
                float alphaMul = (1.0f / (index + 1)) * 0.7f;
                int layerAlpha = Math.max(1, Math.min(255, Math.round(alpha(color) * alphaMul)));
                drawOutlineParts(matrices, model, layerExpand, withAlpha(color, layerAlpha));
            }
        }

        RenderSystem.defaultBlendFunc();
        drawOutlineParts(matrices, model, expand, color);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private void drawOutlineParts(MatrixStack matrices, BipedEntityModel<?> model, float expand, int color) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        var root = model.getRootPart();
        renderPartOutlineLines(matrices, buffer, root, model.head, -4, -8, -4, 8, 8, 8, expand, color);
        renderPartOutlineLines(matrices, buffer, root, model.body, -4, 0, -2, 8, 12, 4, expand, color);
        renderPartOutlineLines(matrices, buffer, root, model.rightArm, -3, -2, -2, 4, 12, 4, expand, color);
        renderPartOutlineLines(matrices, buffer, root, model.leftArm, -1, -2, -2, 4, 12, 4, expand, color);
        renderPartOutlineLines(matrices, buffer, root, model.rightLeg, -2, 0, -2, 4, 12, 4, expand, color);
        renderPartOutlineLines(matrices, buffer, root, model.leftLeg, -2, 0, -2, 4, 12, 4, expand, color);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderPartOutlineLines(MatrixStack baseStack, BufferBuilder buffer, net.minecraft.client.model.ModelPart root, net.minecraft.client.model.ModelPart part,
                                        float offX, float offY, float offZ, float width, float height, float depth, float expand, int color) {
        baseStack.push();
        root.rotate(baseStack);
        part.rotate(baseStack);

        float scale = 1f / 16f;
        float expandScale = expand * scale;
        float minX = offX * scale - expandScale;
        float minY = offY * scale - expandScale;
        float minZ = offZ * scale - expandScale;
        float maxX = (offX + width) * scale + expandScale;
        float maxY = (offY + height) * scale + expandScale;
        float maxZ = (offZ + depth) * scale + expandScale;
        Matrix4f matrix = baseStack.peek().getPositionMatrix();

        addLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, color);
        addLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, color);
        addLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, color);
        addLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, color);

        addLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, color);
        addLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        addLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        addLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, color);

        addLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, color);
        addLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, color);
        addLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        addLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, color);

        baseStack.pop();
    }

    private void addLine(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        int r = red(color);
        int g = green(color);
        int b = blue(color);
        int a = alpha(color);

        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }

    private void setUniform(ShaderProgram shader, String name, float value) {
        var uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    public boolean affects(PlayerEntity player) {
        if (!isEnabled() || player == null || !player.isAlive()) return false;
        if (player == mc.player) {
            return rendering.isSelected(TARGET_SELF) && mc.options.getPerspective() != Perspective.FIRST_PERSON;
        }
        if (isFriend(player)) return rendering.isSelected(TARGET_FRIENDS);
        return rendering.isSelected(TARGET_PLAYERS);
    }

    public boolean shouldHideBaseModel(PlayerEntity player) {
        return hideOriginal.getValue() && affects(player);
    }

    public boolean shouldHideItemsAndCape(PlayerEntity player) {
        return hideItemsAndCape.getValue() && affects(player);
    }

    public int resolveFillColor(PlayerEntity player) {
        return applyPulse(baseFillColor(player));
    }

    public int resolveOutlineColor(PlayerEntity player) {
        return applyPulse(baseOutlineColor(player));
    }

    private int baseFillColor(PlayerEntity player) {
        if (isFriend(player)) return FRIEND_FILL_COLOR;
        return vividWithAlpha(ThemeManager.getInstance().getPrimary(), CLIENT_FILL_SATURATION, CLIENT_FILL_BRIGHTNESS, DEFAULT_FILL_ALPHA);
    }

    private int baseOutlineColor(PlayerEntity player) {
        if (isFriend(player)) return FRIEND_OUTLINE_COLOR;
        return vividWithAlpha(ThemeManager.getInstance().getPrimary(), CLIENT_OUTLINE_SATURATION, CLIENT_OUTLINE_BRIGHTNESS, 255);
    }

    private int applyPulse(int color) {
        if (!pulse.getValue()) return color;
        float elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0f;
        float pulseValue = (float) ((Math.sin(elapsedSeconds * pulseSpeed.getValue() * Math.PI) + 1.0) * 0.5);
        float alphaMul = MIN_PULSE_ALPHA + PULSE_SWING * pulseValue;
        return multAlpha(color, alphaMul);
    }

    private int vividWithAlpha(int color, float saturationBoost, float brightnessBoost, int alpha) {
        float[] hsb = Color.RGBtoHSB(red(color), green(color), blue(color), null);
        float saturation = MathHelper.clamp(hsb[1] * saturationBoost, 0.0f, 1.0f);
        float brightness = MathHelper.clamp(Math.max(hsb[2], 0.8f) * brightnessBoost, 0.0f, 1.0f);
        int rgb = Color.HSBtoRGB(hsb[0], saturation, brightness);
        return rgba(red(rgb), green(rgb), blue(rgb), alpha);
    }

    private boolean isFriend(PlayerEntity player) {
        return Cheap.getInstance().getFriendManager().contains(player.getName().getString());
    }

    private static int red(int color) { return (color >> 16) & 0xFF; }
    private static int green(int color) { return (color >> 8) & 0xFF; }
    private static int blue(int color) { return color & 0xFF; }
    private static int alpha(int color) { return (color >> 24) & 0xFF; }
    private static int rgba(int r, int g, int b, int a) { return (a << 24) | (r << 16) | (g << 8) | b; }
    private static int withAlpha(int color, int a) { return (color & 0x00FFFFFF) | ((a & 0xFF) << 24); }
    private static int multAlpha(int color, float mul) {
        int a = Math.round(alpha(color) * mul);
        return (color & 0x00FFFFFF) | (MathHelper.clamp(a, 0, 255) << 24);
    }
}

