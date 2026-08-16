package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.NumberSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.render.providers.ColorProvider;
import fun.ogi.util.animation.Easing;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInformation(moduleName = "Target Esp", moduleCategory = ModuleCategory.RENDER)
public class TargetEsp extends Module {
    private final Identifier SKULL_0 = Identifier.of("cheap", "textures/skull_state_0.png");
    private final Identifier SKULL_1 = Identifier.of("cheap", "textures/skull_1.png");
    private final Identifier SKULL_2 = Identifier.of("cheap", "textures/skull_2.png");
    private final Identifier MARKER = Identifier.of("cheap", "textures/capture2.png");
    private final Identifier MARKER_2 = Identifier.of("cheap","textures/capture.png");
    private final Identifier GLOW_TEXTURE = Identifier.of("cheap", "textures/glow.png");
    private final Animation animation2 = new Animation();
    private final Animation toggleFade = new Animation();
    private LivingEntity target;
    private int hitAnimTicks = 0;
    private int prevHurtTime = 0;
    private static final int HIT_ANIM_DURATION = 8;
    private static final float MAX_HIT_ROTATION = 180f;
    private boolean pendingDisable = false;

    private long timestamp4;
    private long timestamp5;
    private float value23;

    private LivingEntity spiritsLastTarget;
    private float spiritsNurik;
    private long spiritsLastTime;
    private final List<Vec3d> spiritsTrail = new ArrayList<>();
    private static final int TRAIL_MAX_SIZE = 20;
    private static final double TRAIL_MIN_DISTANCE = 0.05;

    private ModeSetting mode = new ModeSetting("Mode:", this, "Marker", "Marker", "Skull", "Spirits", "Circles", "Circle", "Spirits 2", "Nur Spirits","Test");
    private ModeSetting markerMode = new ModeSetting("Marker Mode:",this,"First","First","Second").visible(()->mode.getValueAsString().equals("Marker"));
    private ModeSetting spiritMode = new ModeSetting("Spirit Mode:", this, "Twirl", "Twirl", "Wave", "Pulse");
    private NumberSetting animSpeed = new NumberSetting("Anim Speed", this, 2.0, 0.5, 10.0, 0.5);
    private BooleanSetting gradient = new BooleanSetting("Gradient", this, false);

    public TargetEsp() {
        addSetting(mode);
        addSetting(markerMode);
        addSetting(spiritMode);
        addSetting(animSpeed);
        addSetting(gradient);
        this.timestamp4 = System.currentTimeMillis();
        this.timestamp5 = System.nanoTime();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.isEnabled() == enabled) return;
        if (!enabled) {
            toggleFade.start(toggleFade.getValue(), 0f, 250, Easing.CUBIC_OUT);
            pendingDisable = true;
            return;
        }
        pendingDisable = false;
        toggleFade.setValue(0f);
        toggleFade.start(0f, 1f, 250, Easing.CUBIC_OUT);
        super.setEnabled(true);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        pendingDisable = false;
        spiritsLastTime = 0;
        spiritsNurik = 0;
        spiritsTrail.clear();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        toggleFade.update();

        if (pendingDisable && toggleFade.isFinished()) {
            pendingDisable = false;
            super.setEnabled(false);
            return;
        }

        if (mc.world == null || mc.player == null) return;

        Entity crosshair = null;
        if (mc.crosshairTarget instanceof EntityHitResult hit) {
            crosshair = hit.getEntity();
        }

        
        if (crosshair instanceof LivingEntity living && living != mc.player && !Cheap.getInstance().getFriendManager().contains(living.getName().getString())) {
            target = living;
        }

        if (target != null && (target.isRemoved() || !target.isAlive() || mc.player.distanceTo(target) > 10 || Cheap.getInstance().getFriendManager().contains(target.getName().getString()))) {
            target = null;
        }

        if (target != null) {
            if (!animation2.isRunning() && animation2.getValue() < 1f) {
                animation2.start(animation2.getValue(), 1.0f, 250, Easing.CUBIC_OUT);
            }
            if (target.hurtTime > prevHurtTime) hitAnimTicks = HIT_ANIM_DURATION;
            prevHurtTime = target.hurtTime;
        } else {
            if (animation2.getValue() > 0f && !animation2.isRunning()) {
                animation2.start(animation2.getValue(), 0.0f, 250, Easing.CUBIC_OUT);
            }
        }
        animation2.update();
        if (hitAnimTicks > 0) hitAnimTicks--;

        if (target != null) {
            Vec3d pos = target.getPos();
            if (spiritsTrail.isEmpty() || spiritsTrail.get(spiritsTrail.size() - 1).distanceTo(pos) > TRAIL_MIN_DISTANCE) {
                spiritsTrail.add(pos.add(0, target.getHeight() / 2.0, 0));
                if (spiritsTrail.size() > TRAIL_MAX_SIZE) spiritsTrail.remove(0);
            }
        } else {
            if (!spiritsTrail.isEmpty()) spiritsTrail.remove(0);
        }
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.world == null || mc.player == null || toggleFade.getValue() <= 0.001) return;

        animation2.update();

        if (mode.getValueAsString().equals("Spirits 2")) {
            renderSpirits2(event);
            return;
        }

        if (mode.getValueAsString().equals("Nur Spirits")) {
            renderNurSpirits(event);
            return;
        }

        if (target == null || animation2.getValue() <= 0.001) return;

        if (mode.getValueAsString().equals("Marker")) renderMarker(event);
        if (mode.getValueAsString().equals("Skull")) renderSkull(event);
        if (mode.getValueAsString().equals("Spirits")) renderSpirits(event);
        if (mode.getValueAsString().equals("Circles")) renderCircles(event);
        if (mode.getValueAsString().equals("Circle")) renderCircle(event);
        if(mode.getValueAsString().equals("Test"))renderTest(event);
    }

    public void renderMarker(EventWorldRenderer event) {
        float pt = event.getRenderTickCounter().getTickDelta(true);

        double ex = MathHelper.lerp(pt, target.prevX, target.getX());
        double ey = MathHelper.lerp(pt, target.prevY, target.getY()) + target.getHeight() / 2f;
        double ez = MathHelper.lerp(pt, target.prevZ, target.getZ());

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = event.getCamera().getPos();

        MatrixStack ms = new MatrixStack();
        ms.translate(ex - camPos.x, ey - camPos.y, ez - camPos.z);

        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cam.getYaw()));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));

        long now = System.currentTimeMillis();
        float spinDegreesPerSecond = 90f;
        float spinAngle = ((now % 100000L) / 1000f) * spinDegreesPerSecond * animSpeed.getFloatValue();
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spinAngle));

        float a = animation2.getValue();
        ms.scale(-1f, -1f, 1f);

        float toggleA = toggleFade.getValue();
        Color base = target.hurtTime > 0
                ? Color.RED
                : new Color(ThemeManager.getInstance().getPalette().getPrimary());

        Color color = new Color(
                base.getRed(),
                base.getGreen(),
                base.getBlue(),
                (int) (255 * a * toggleA)
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        if(markerMode.getValueAsString().equals("First")){
            RenderSystem.setShaderTexture(0, MARKER);
        }else{
            RenderSystem.setShaderTexture(0, MARKER_2);
        }


        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR
        );

        if (gradient.getValue() && target.hurtTime == 0) {
            drawQuad(buffer, ms.peek().getPositionMatrix(), 0.5f, espColor(0f, color.getAlpha()), espColor(1f, color.getAlpha()));
        } else {
            drawQuad(buffer, ms.peek().getPositionMatrix(), 0.5f, color, color);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public void renderSkull(EventWorldRenderer event) {
        float toggleA = toggleFade.getValue();
        float pt = event.getRenderTickCounter().getTickDelta(false);

        double ex = MathHelper.lerp(pt, target.prevX, target.getX());
        double ey = MathHelper.lerp(pt, target.prevY, target.getY()) + target.getHeight() / 2f;
        double ez = MathHelper.lerp(pt, target.prevZ, target.getZ());

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = event.getCamera().getPos();

        MatrixStack ms = new MatrixStack();
        ms.translate(ex - camPos.x, ey - camPos.y, ez - camPos.z);

        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cam.getYaw()));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));

        float a = animation2.getValue();

        ms.scale(-1f, -1f, 1f);

        Color base = target.hurtTime > 0
                ? Color.RED
                : new Color(ThemeManager.getInstance().getPalette().getPrimary());

        Color color = new Color(
                base.getRed(),
                base.getGreen(),
                base.getBlue(),
                (int) (255 * a * toggleA)
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, SKULL_0);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR
        );

        if (gradient.getValue() && target.hurtTime == 0) {
            drawQuad(buffer, ms.peek().getPositionMatrix(), 0.6f, espColor(0f, color.getAlpha()), espColor(1f, color.getAlpha()));
        } else {
            drawQuad(buffer, ms.peek().getPositionMatrix(), 0.6f, color, color);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public void renderSpirits(EventWorldRenderer event) {
        float toggleA = toggleFade.getValue();
        float pt = event.getRenderTickCounter().getTickDelta(false);
        double x = MathHelper.lerp(pt, target.prevX, target.getX());
        double y = MathHelper.lerp(pt, target.prevY, target.getY()) + target.getHeight() / 2f;
        double z = MathHelper.lerp(pt, target.prevZ, target.getZ());

        float hurtTime = 0f;
        if (target instanceof LivingEntity living) {
            hurtTime = ((float) living.hurtTime - (living.hurtTime != 0 ? pt : 0f)) / 10f;
        }

        float animValue = -0.15f * animation2.getValue() + 0.65f;
        float speed = animSpeed.getFloatValue();
        long time = (long) ((System.currentTimeMillis() - timestamp4) / speed);
        long nanoTime = System.nanoTime();
        float deltaTime = (nanoTime - timestamp5) / 2000000f;
        timestamp5 = nanoTime;
        value23 += hurtTime * deltaTime;

        Camera cam = mc.gameRenderer.getCamera();
        MatrixStack ms = new MatrixStack();
        Vec3d camPos = event.getCamera().getPos();
        ms.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        ms.scale(1.5f, 1.5f, 1.5f);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        String sMode = spiritMode.getValue();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < 14; i++) {
                ms.push();
                float progress = (float) i / 13f;
                float size = (0.55f * (1f - progress) + 0.2f * progress) * animation2.getValue() * toggleA;
                double angle = 0.2f * (time + value23 - (float) i * 7f) / 15f;

                boolean firstHalf = progress < 0.5f;
                float wave = firstHalf ? progress * 2f : (1f - progress) * 2f;
                double amplitude = Math.sin(wave * Math.PI) * 2.0;

                Random random = new Random((long) i * 12345L);
                double offsetX = (random.nextDouble() - 0.5) * amplitude;
                double offsetY = (random.nextDouble() - 0.5) * amplitude;
                double offsetZ = (random.nextDouble() - 0.5) * amplitude;

                double animOffsetX = offsetX * animation2.getValue() - offsetX;
                double animOffsetY = offsetY * animation2.getValue() - offsetY;
                double animOffsetZ = offsetZ * animation2.getValue() - offsetZ;

                switch (sMode) {
                    case "Wave" -> {
                        double waveOffset = Math.sin(time / 20.0 + i * 0.5) * 0.3;
                        ms.translate(Math.sin(angle) * animValue + animOffsetX, waveOffset + animOffsetY, Math.cos(angle) * animValue + animOffsetZ);
                    }
                    case "Pulse" -> {
                        double pulse = Math.sin(time / 15.0 + i * 0.8) * 0.2 + 0.8;
                        ms.translate(Math.sin(angle) * animValue * pulse + animOffsetX, animOffsetY, Math.cos(angle) * animValue * pulse + animOffsetZ);
                    }
                    default -> {
                        double posX = -Math.sin(angle) * animValue;
                        double posZ = -Math.cos(angle) * animValue;
                        switch (layer) {
                            case 0:
                                animOffsetY += (double) i * 0.02;
                                ms.translate(posX + animOffsetX, posZ + animOffsetY, -posZ + animOffsetZ);
                                break;
                            case 1:
                                animOffsetY -= (double) i * 0.02;
                                ms.translate(-posX + animOffsetX, posX + animOffsetY, -posZ + animOffsetZ);
                                break;
                            case 2:
                                ms.translate(-posX + animOffsetX, -posX + animOffsetY, posZ + animOffsetZ);
                        }
                    }
                }

                float particleSize = size * 0.50f;
                int col = espRGB(progress);
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;
                int a = Math.min(255, (int) (600f * animation2.getValue() * toggleA));

                ms.multiply(mc.gameRenderer.getCamera().getRotation());

                buffer.vertex(ms.peek().getPositionMatrix(), -particleSize, -particleSize, 0).texture(1f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), particleSize, -particleSize, 0).texture(0f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), particleSize, particleSize, 0).texture(0f, 0f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), -particleSize, particleSize, 0).texture(1f, 0f).color(r, g, b, a);

                ms.pop();
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(770, 771);
        RenderSystem.enableCull();
    }

    private void renderCircles(EventWorldRenderer event) {
        float toggleA = toggleFade.getValue();
        float pt = event.getRenderTickCounter().getTickDelta(false);
        double x = MathHelper.lerp(pt, target.prevX, target.getX());
        double y = MathHelper.lerp(pt, target.prevY, target.getY());
        double z = MathHelper.lerp(pt, target.prevZ, target.getZ());

        float height = target.getHeight();
        float speed = animSpeed.getFloatValue();
        short period = (short) (1500 * speed);
        double time = System.currentTimeMillis() % period;
        boolean ascending = time > period / 2;
        float progress = (float) (time / ((float) period / 2f));

        if (ascending) progress -= 1f;
        else progress = 1f - progress;

        progress = progress < 0.5f ? 2f * progress * progress :
                1f - (float) Math.pow(-2f * progress + 2f, 2) / 2f;

        double yOffset = height / 2f * (progress > 0.5f ? 1f - progress : progress) * (ascending ? -1 : 1);

        Camera cam = mc.gameRenderer.getCamera();
        MatrixStack ms = new MatrixStack();
        Vec3d camPos = event.getCamera().getPos();
        ms.translate(x - camPos.x, y + height * progress + yOffset - camPos.y, z - camPos.z);

        float hurtTime = 0f;
        if (target instanceof LivingEntity living) {
            hurtTime = ((float) living.hurtTime - (living.hurtTime != 0 ? pt : 0f)) / 10f;
        }

        long timeMs = (long) ((System.currentTimeMillis() - timestamp4) / speed);
        long nanoTime = System.nanoTime();
        float deltaTime = (nanoTime - timestamp5) / 2000000f;
        timestamp5 = nanoTime;
        value23 += hurtTime * deltaTime;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int layer = 0; layer < 4; layer++) {
            for (int i = 0; i < 15; i++) {
                ms.push();
                float particleProgress = (float) i / 14f;
                float size = (0.5f * (1f - particleProgress) + 0.5f * particleProgress) * animation2.getValue() * toggleA;
                float angle = 0.2f * (timeMs + value23 - (float) i * 3.5f) / 15f;

                boolean firstHalf = particleProgress < 0.5f;
                float wave = firstHalf ? particleProgress * 2f : (1f - particleProgress) * 2f;
                double amplitude = Math.sin(wave * Math.PI) * 2.0;

                Random random = new Random((long) i * 12345L);
                double offsetX = (random.nextDouble() - 0.5) * amplitude;
                double offsetY = (random.nextDouble() - 0.5) * amplitude;
                double offsetZ = (random.nextDouble() - 0.5) * amplitude;

                double animOffsetX = offsetX * animation2.getValue() - offsetX;
                double animOffsetY = offsetY * animation2.getValue() - offsetY;
                double animOffsetZ = offsetZ * animation2.getValue() - offsetZ;

                double radius = 0.7;
                switch (layer) {
                    case 0:
                        ms.translate(Math.cos(angle) * radius + animOffsetX, animOffsetY, Math.sin(angle) * radius + animOffsetZ);
                        break;
                    case 1:
                        ms.translate(-Math.sin(angle) * radius + animOffsetX, animOffsetY, Math.cos(angle) * radius + animOffsetZ);
                        break;
                    case 2:
                        ms.translate(-Math.cos(angle) * radius + animOffsetX, animOffsetY, -Math.sin(angle) * radius + animOffsetZ);
                        break;
                    case 3:
                        ms.translate(Math.sin(angle) * radius + animOffsetX, animOffsetY, -Math.cos(angle) * radius + animOffsetZ);
                }

                float particleSize = size * 0.5f;
                int col = espRGB(particleProgress);
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;
                int a = Math.min(255, (int) (400f * animation2.getValue() * toggleA));

                ms.multiply(mc.gameRenderer.getCamera().getRotation());

                buffer.vertex(ms.peek().getPositionMatrix(), -particleSize, -particleSize, 0).texture(1f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), particleSize, -particleSize, 0).texture(0f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), particleSize, particleSize, 0).texture(0f, 0f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), -particleSize, particleSize, 0).texture(1f, 0f).color(r, g, b, a);

                ms.pop();
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(770, 771);
        RenderSystem.enableCull();
    }

    private void renderCircle(EventWorldRenderer event) {
        float pt = event.getRenderTickCounter().getTickDelta(false);

        double ex = MathHelper.lerp(pt, target.prevX, target.getX());
        double ey = MathHelper.lerp(pt, target.prevY, target.getY());
        double ez = MathHelper.lerp(pt, target.prevZ, target.getZ());

        float height = target.getHeight();
        float speed = animSpeed.getFloatValue();
        short period = (short) (2000 * speed);

        long now = System.currentTimeMillis();
        double elapsed = now % period;
        boolean side = elapsed > period / 2.0;
        double progress = elapsed / ((double) period / 2.0);
        progress = side ? progress - 1.0 : 1.0 - progress;
        progress = progress < 0.5 ? 2.0 * progress * progress
                : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;

        
        double eased = (height / 2.0) * (progress > 0.5 ? 1.0 - progress : progress) * (side ? -1 : 1);

        double baseY = ey + height * progress;

        float a = animation2.getValue() * toggleFade.getValue();
        float alphaMult = Math.min(1f, a);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        int topCol = espRGB(0f);
        int botCol = espRGB(1f);
        int tr = (topCol >> 16) & 0xFF;
        int tg = (topCol >> 8) & 0xFF;
        int tb = topCol & 0xFF;
        int br = (botCol >> 16) & 0xFF;
        int bg = (botCol >> 8) & 0xFF;
        int bb = botCol & 0xFF;

        int glowAlpha = (int) (200 * alphaMult); 
        int coreAlpha = (int) (25 * alphaMult);  

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float ringSize = target.getWidth() * 0.85f;
        int segments = 60;

        Vec3d camPos = event.getCamera().getPos();
        Matrix4f mat = new Matrix4f().translate((float) (ex - camPos.x), (float) (baseY - camPos.y), (float) (ez - camPos.z));

        float yTop = 0f;
        float yBottom = (float) (eased * 1.5);

        for (int i = 0; i < segments; i++) {
            double a1 = Math.toRadians(i * (360.0 / segments));
            double a2 = Math.toRadians((i + 1) * (360.0 / segments));
            float x1 = (float) (Math.cos(a1) * ringSize);
            float z1 = (float) (Math.sin(a1) * ringSize);
            float x2 = (float) (Math.cos(a2) * ringSize);
            float z2 = (float) (Math.sin(a2) * ringSize);

            
            buffer.vertex(mat, x1, yTop, z1).color(tr, tg, tb, glowAlpha);
            buffer.vertex(mat, x2, yTop, z2).color(tr, tg, tb, glowAlpha);
            buffer.vertex(mat, x2, yBottom, z2).color(br, bg, bb, coreAlpha);
            buffer.vertex(mat, x1, yBottom, z1).color(br, bg, bb, coreAlpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        
        RenderSystem.lineWidth(1.5f);
        BufferBuilder lineBuffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double ang = Math.toRadians(i * (360.0 / segments));
            float x = (float) (Math.cos(ang) * ringSize);
            float z = (float) (Math.sin(ang) * ringSize);
            lineBuffer.vertex(mat, x, 0, z).color(tr, tg, tb, glowAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(lineBuffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderSpirits2(EventWorldRenderer event) {
        float toggleA = toggleFade.getValue();
        float pt = event.getRenderTickCounter().getTickDelta(false);
        float animValue = -0.15f * animation2.getValue() + 0.65f;
        float speed = animSpeed.getFloatValue();
        long time = (long) ((System.currentTimeMillis() - timestamp4) / speed);
        long nanoTime = System.nanoTime();
        float deltaTime = (nanoTime - timestamp5) / 2000000f;
        if (target != null) spiritsLastTarget = target;
        if (spiritsLastTarget == null || animation2.getValue() <= 0.001f) return;

        long now = System.currentTimeMillis();
        if (spiritsLastTime == 0) spiritsLastTime = now;
        spiritsNurik += (float) (now - spiritsLastTime) / 120.0f;
        spiritsLastTime = now;

        Camera cam = mc.gameRenderer.getCamera();
        float a2 = animation2.getValue();

        double cx = MathHelper.lerp(pt, spiritsLastTarget.prevX, spiritsLastTarget.getX());
        double cy = MathHelper.lerp(pt, spiritsLastTarget.prevY, spiritsLastTarget.getY()) + spiritsLastTarget.getHeight() / 2.0;
        double cz = MathHelper.lerp(pt, spiritsLastTarget.prevZ, spiritsLastTarget.getZ());

        int baseR, baseG, baseB;
        if (spiritsLastTarget.hurtTime > 0) {
            baseR = 255;
            baseG = 60;
            baseB = 60;
        } else {
            int primary = ThemeManager.getInstance().getPalette().getPrimary();
            baseR = (primary >> 16) & 0xFF;
            baseG = (primary >> 8) & 0xFF;
            baseB = primary & 0xFF;
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);





























        double x = MathHelper.lerp(pt, target.prevX, target.getX());
        double y = MathHelper.lerp(pt, target.prevY, target.getY()) + target.getHeight() / 2f;
        double z = MathHelper.lerp(pt, target.prevZ, target.getZ());

        float hurtTime = 0f;
        if (target instanceof LivingEntity living) {
            hurtTime = ((float) living.hurtTime - (living.hurtTime != 0 ? pt : 0f)) / 10f;
        }

        timestamp5 = nanoTime;
        value23 += hurtTime * deltaTime;


        MatrixStack ms = new MatrixStack();
        Vec3d camPos = event.getCamera().getPos();
        ms.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        ms.scale(1.5f, 1.5f, 1.5f);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        String sMode = spiritMode.getValue();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < 14; i++) {
                ms.push();
                float progress = (float) i / 13f;
                float size = (0.40f * (1f - progress) + 0.2f * progress) * animation2.getValue() * toggleA;
                double angle = 0.2f * (time + value23 - (float) i * 7f) / 15f;

                boolean firstHalf = progress < 0.5f;
                float wave = firstHalf ? progress * 2f : (1f - progress) * 2f;
                double amplitude = Math.sin(wave * Math.PI) * 2.0;

                Random random = new Random((long) i * 12345L);
                double offsetX = (random.nextDouble() - 0.5) * amplitude;
                double offsetY = (random.nextDouble() - 0.5) * amplitude;
                double offsetZ = (random.nextDouble() - 0.5) * amplitude;

                double animOffsetX = offsetX * animation2.getValue() - offsetX;
                double animOffsetY = offsetY * animation2.getValue() - offsetY;
                double animOffsetZ = offsetZ * animation2.getValue() - offsetZ;

                switch (sMode) {
                    case "Wave" -> {
                        double waveOffset = Math.sin(time / 20.0 + i * 0.5) * 0.3;
                        ms.translate(Math.sin(angle) * animValue + animOffsetX, waveOffset + animOffsetY, Math.cos(angle) * animValue + animOffsetZ);
                    }
                    case "Pulse" -> {
                        double pulse = Math.sin(time / 15.0 + i * 0.8) * 0.2 + 0.8;
                        ms.translate(Math.sin(angle) * animValue * pulse + animOffsetX, animOffsetY, Math.cos(angle) * animValue * pulse + animOffsetZ);
                    }
                    default -> {
                        double posX = -Math.sin(angle) * animValue;
                        double posZ = -Math.cos(angle) * animValue;
                        switch (layer) {
                            case 0:
                                animOffsetY += (double) i * 0.02;
                                ms.translate(posX + animOffsetX, posZ + animOffsetY, -posZ + animOffsetZ);
                                break;
                            case 1:
                                animOffsetY -= (double) i * 0.02;
                                ms.translate(-posX + animOffsetX, posX + animOffsetY, -posZ + animOffsetZ);
                                break;
                            case 2:
                                ms.translate(-posX + animOffsetX, -posX + animOffsetY, posZ + animOffsetZ);
                        }
                    }
                }

                float particleSize = size * 0.35f;
                int col = espRGB(progress);
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;
                int a = Math.min(255, (int) (600f * animation2.getValue() * toggleA));

                ms.multiply(mc.gameRenderer.getCamera().getRotation());

                buffer.vertex(ms.peek().getPositionMatrix(), -particleSize, -particleSize, 0).texture(1f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), particleSize, -particleSize, 0).texture(0f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), particleSize, particleSize, 0).texture(0f, 0f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), -particleSize, particleSize, 0).texture(1f, 0f).color(r, g, b, a);

                ms.pop();
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(770, 771);
        RenderSystem.enableCull();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(770, 771);
        RenderSystem.enableCull();

    }

    private void renderNurSpirits(EventWorldRenderer event) {
        LivingEntity renderTarget = target;
        
        if (renderTarget == null) return;

        float pt = event.getRenderTickCounter().getTickDelta(false);

        double ex = MathHelper.lerp(pt, renderTarget.prevX, renderTarget.getX());
        double ey = MathHelper.lerp(pt, renderTarget.prevY, renderTarget.getY()) + renderTarget.getHeight() / 2.0;
        double ez = MathHelper.lerp(pt, renderTarget.prevZ, renderTarget.getZ());

        Camera cam = mc.gameRenderer.getCamera();

        float alpha = toggleFade.getValue();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        long now = System.currentTimeMillis();
        double radius = 0.6;
        float particleSize = 0.32f;
        int length = 25;

        Tessellator tessellator = Tessellator.getInstance();
        for (int pass = 0; pass < 3; pass++) {
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (int i = 0; i < length; i++) {
                double angle = 0.15 * (now - i * 15) / 27;
                double s = Math.sin(angle) * radius;
                double c = Math.cos(angle) * radius;

                float dx = 0, dy = 0, dz = 0;
                switch (pass) {
                    case 0 -> { dx = (float) s; dy = (float) c; dz = (float) -c; }
                    case 1 -> { dx = (float) -s; dy = (float) s; dz = (float) -c; }
                    case 2 -> { dx = (float) -s; dy = (float) -s; dz = (float) c; }
                }

                MatrixStack ms = new MatrixStack();
                Vec3d camPos = event.getCamera().getPos();
                ms.translate(ex - camPos.x + dx, ey - camPos.y + dy, ez - camPos.z + dz);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cam.getYaw()));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));

                float size = particleSize * alpha;
                int a = Math.min(255, (int) (alpha * Math.max(0, 255 - i * 10)));
                if (a < 3) continue;

                float progress = length > 1 ? (float) i / (length - 1) : 0f;
                int col = espRGB(progress);
                int r = (col >> 16) & 0xFF;
                int g = (col >> 8) & 0xFF;
                int b = col & 0xFF;
                if (renderTarget.hurtTime > 0) { r = 255; g = 60; b = 60; }

                buffer.vertex(ms.peek().getPositionMatrix(), -size, -size, 0).texture(1f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), size, -size, 0).texture(0f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), size, size, 0).texture(0f, 0f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), -size, size, 0).texture(1f, 0f).color(r, g, b, a);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.blendFunc(770, 771);
        RenderSystem.enableCull();
    }
    private void renderTest(EventWorldRenderer event){
        LivingEntity renderTarget = target;
        if (renderTarget == null) return;

        float pt = event.getRenderTickCounter().getTickDelta(false);
        double ex = MathHelper.lerp(pt, renderTarget.prevX, renderTarget.getX());
        double ey = MathHelper.lerp(pt, renderTarget.prevY, renderTarget.getY()) + renderTarget.getHeight() / 2.0;
        double ez = MathHelper.lerp(pt, renderTarget.prevZ, renderTarget.getZ());

        float alpha = toggleFade.getValue() * animation2.getValue();

        float hurtTime = 0f;
        if (renderTarget instanceof LivingEntity living) {
            hurtTime = ((float) living.hurtTime - (living.hurtTime != 0 ? pt : 0f)) / 10f;
        }

        long timeMs = (long) ((System.currentTimeMillis() - timestamp4) / animSpeed.getFloatValue());
        long nanoTime = System.nanoTime();
        float deltaTime = (nanoTime - timestamp5) / 2000000f;
        timestamp5 = nanoTime;
        value23 += hurtTime * deltaTime;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        double baseRadius = 0.7;
        float particleSize = 0.18f;
        int count = 26;

        Vec3d camPos = event.getCamera().getPos();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Random random = new Random(1337L);
        float hurtTilt = 15f + hurtTime * 25f;
        for (int i = 0; i < count; i++) {
            double base = 0.2f * (timeMs + value23) / 15f;
            double radius = baseRadius * (0.55 + random.nextDouble() * 1.1);
            double heightOff = (random.nextDouble() - 0.5) * renderTarget.getHeight() * 1.1;
            double speedFactor = 0.7 + random.nextDouble() * 0.6;
            double angle = base * speedFactor + random.nextDouble() * Math.PI * 2.0;
            double px = Math.cos(angle) * radius;
            double pz = Math.sin(angle) * radius;

            MatrixStack ms = new MatrixStack();
            ms.translate(ex - camPos.x + px, ey - camPos.y + heightOff, ez - camPos.z + pz);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) Math.toDegrees(angle) + i * 37f));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(hurtTilt));

            float size = particleSize * (0.5f + (float) random.nextDouble() * 1.2f) * alpha;
            int a = Math.min(255, (int) (255 * alpha));
            if (a < 3) continue;

            float progress = count > 1 ? (float) i / (count - 1) : 0f;
            int col = espRGB(progress);
            int r = (col >> 16) & 0xFF;
            int g = (col >> 8) & 0xFF;
            int b = col & 0xFF;
            if (renderTarget.hurtTime > 0) { r = 255; g = 60; b = 60; }

            drawDiamond(buffer, ms.peek().getPositionMatrix(), size, r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
    private void drawDiamond(BufferBuilder buffer, Matrix4f matrix, float size, int r, int g, int b, int a) {
        float top = size;
        float bottom = -size;
        float s = size * 0.6f;

        // верхние 4 грани
        addTriShaded(buffer, matrix, 0, top, 0,  s, 0, 0,   0, 0, s,  r, g, b, a);
        addTriShaded(buffer, matrix, 0, top, 0,  0, 0, s,  -s, 0, 0,  r, g, b, a);
        addTriShaded(buffer, matrix, 0, top, 0, -s, 0, 0,   0, 0, -s, r, g, b, a);
        addTriShaded(buffer, matrix, 0, top, 0,  0, 0, -s,  s, 0, 0,  r, g, b, a);

        // нижние 4 грани
        addTriShaded(buffer, matrix, s, 0, 0,   0, bottom, 0,  0, 0, s,  r, g, b, a);
        addTriShaded(buffer, matrix, 0, 0, s,   0, bottom, 0, -s, 0, 0,  r, g, b, a);
        addTriShaded(buffer, matrix, -s, 0, 0,  0, bottom, 0,  0, 0, -s, r, g, b, a);
        addTriShaded(buffer, matrix, 0, 0, -s,  0, bottom, 0,  s, 0, 0,  r, g, b, a);
    }

    // фиксированное направление "света" для fake-shading
    private static final org.joml.Vector3f LIGHT_DIR = new org.joml.Vector3f(0.4f, 0.9f, 0.3f).normalize();

    private void addTriShaded(BufferBuilder buffer, Matrix4f matrix,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              int r, int g, int b, int a) {
        // нормаль грани через векторное произведение рёбер
        float ux = x2 - x1, uy = y2 - y1, uz = z2 - z1;
        float vx = x3 - x1, vy = y3 - y1, vz = z3 - z1;
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0001f) { nx /= len; ny /= len; nz /= len; }

        float dot = nx * LIGHT_DIR.x + ny * LIGHT_DIR.y + nz * LIGHT_DIR.z;
        // диапазон яркости от 0.45 (тень) до 1.0 (свет)
        float shade = 0.45f + 0.55f * Math.max(0f, dot);

        int sr = Math.min(255, (int) (r * shade));
        int sg = Math.min(255, (int) (g * shade));
        int sb = Math.min(255, (int) (b * shade));

        buffer.vertex(matrix, x1, y1, z1).color(sr, sg, sb, a);
        buffer.vertex(matrix, x2, y2, z2).color(sr, sg, sb, a);
        buffer.vertex(matrix, x3, y3, z3).color(sr, sg, sb, a);
    }
    private void addTri(BufferBuilder buffer, Matrix4f matrix,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        int r, int g, int b, int a) {
        // BufferBuilder с DrawMode.TRIANGLES ждёт по 3 вершины на треугольник
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a);
    }
    private int espRGB(float progress) {
        if (!gradient.getValue()) {
            return ThemeManager.getInstance().getPrimary();
        }
        ThemeManager tm = ThemeManager.getInstance();
        int start = tm.getPrimary();
        int end = tm.isRainbow() ? start : tm.getSecondary();
        return ColorProvider.interpolateColor(start, end, progress);
    }

    private Color espColor(float progress, int alpha) {
        int col = espRGB(progress);
        return new Color((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, alpha);
    }

    private void drawQuad(BufferBuilder buffer, Matrix4f matrix, float size, Color top, Color bottom) {
        buffer.vertex(matrix, -size, -size, 0).texture(0, 0).color(top.getRed(), top.getGreen(), top.getBlue(), top.getAlpha());
        buffer.vertex(matrix, -size, size, 0).texture(0, 1).color(bottom.getRed(), bottom.getGreen(), bottom.getBlue(), bottom.getAlpha());
        buffer.vertex(matrix, size, size, 0).texture(1, 1).color(bottom.getRed(), bottom.getGreen(), bottom.getBlue(), bottom.getAlpha());
        buffer.vertex(matrix, size, -size, 0).texture(1, 0).color(top.getRed(), top.getGreen(), top.getBlue(), top.getAlpha());
    }
}