package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.render.math.MathUtil;
import fun.ogi.util.time.Timer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInformation(moduleName = "Fire Fly", moduleDesc = "Pretty firefly particles", moduleCategory = ModuleCategory.RENDER)
public class FireFly extends Module {

    private static final Identifier BLOOM = Identifier.of("cheap", "textures/particle/bloom.png");

    private final SliderSetting count = new SliderSetting("Count", this, 50, 10, 300, 10);
    private final SliderSetting speed = new SliderSetting("Speed", this, 0.15, 0.05, 0.5, 0.05);
    private final SliderSetting radius = new SliderSetting("Spawn Radius", this, 25, 10, 50, 5);
    private final SliderSetting trailLength = new SliderSetting("Trail Length", this, 20, 5, 40, 5);
    private final BooleanSetting randomColor = new BooleanSetting("Random Color", this, true);

    private final List<FlyEntity> particles = new ArrayList<>();

    public FireFly() {
        addSettings(count, speed, radius, trailLength, randomColor);
    }

    @Override
    public void onEnable() {
        particles.clear();
    }

    @Override
    public void onDisable() {
        particles.clear();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        Iterator<FlyEntity> it = particles.iterator();
        while (it.hasNext()) {
            FlyEntity p = it.next();
            p.update();
            if (p.timer.finished(8000) || p.getDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ()) >= 60) {
                it.remove();
            }
        }

        while (particles.size() < count.getIntValue()) {
            spawnParticle();
        }
    }

    private void spawnParticle() {
        if (mc.player == null) return;

        double dist = MathUtil.random(5.0, radius.getValue());
        double yawRad = Math.toRadians(MathUtil.random(0.0, 360.0));
        double xOffset = -Math.sin(yawRad) * dist;
        double zOffset = Math.cos(yawRad) * dist;
        double yOffset = MathUtil.random(-5.0, 10.0);

        double vSpeed = speed.getValue();
        double vYaw = Math.toRadians(MathUtil.random(0.0, 360.0));
        double vPitch = Math.toRadians(MathUtil.random(-30.0, 30.0));

        double vx = -Math.sin(vYaw) * Math.cos(vPitch) * vSpeed;
        double vy = Math.sin(vPitch) * vSpeed * 0.5;
        double vz = Math.cos(vYaw) * Math.cos(vPitch) * vSpeed;

        int c;
        if (randomColor.getValue()) {
            float hue = ThreadLocalRandom.current().nextFloat();
            c = Color.HSBtoRGB(hue, 1.0f, 1.0f);
        } else {
            c = ThemeManager.getInstance().getPrimary();
        }

        particles.add(new FlyEntity(
                mc.player.getX() + xOffset,
                mc.player.getY() + yOffset,
                mc.player.getZ() + zOffset,
                vx, vy, vz,
                c
        ));
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.player == null || mc.world == null || particles.isEmpty()) return;

        MatrixStack matrices = event.getMatrices();
        Vec3d cam = event.getCamera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        RenderSystem.setShaderTexture(0, BLOOM);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (FlyEntity p : particles) {
            renderTrail(matrices, p, cam);
            renderParticle(matrices, p, cam);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderTrail(MatrixStack matrices, FlyEntity p, Vec3d cam) {
        List<double[]> trail = p.trail;
        if (trail.size() < 2) return;

        int baseAlpha = getAlpha(p);
        int colorRgb = getFlyColor(p);

        for (int i = 0; i < trail.size(); i++) {
            double[] pos = trail.get(i);
            float fade = (float) i / (float) trail.size();
            float size = 0.15f * fade;
            int trailAlpha = (int) (baseAlpha * fade * 0.8f);

            renderTexturedBillboard(matrices, pos[0], pos[1], pos[2], size, colorRgb, trailAlpha, cam);

            if (i % 3 == 0 && fade > 0.3f) {
                int miniCount = 2 + (int) (Math.random() * 3);
                for (int j = 0; j < miniCount; j++) {
                    double ox = (Math.random() - 0.5) * 0.3;
                    double oy = (Math.random() - 0.5) * 0.3;
                    double oz = (Math.random() - 0.5) * 0.3;
                    float miniSize = 0.04f + (float) (Math.random() * 0.03f);
                    int miniAlpha = (int) (trailAlpha * 0.6f);
                    renderTexturedBillboard(matrices, pos[0] + ox, pos[1] + oy, pos[2] + oz, miniSize, colorRgb, miniAlpha, cam);
                }
            }
        }
    }

    private void renderParticle(MatrixStack matrices, FlyEntity p, Vec3d cam) {
        int baseAlpha = getAlpha(p);
        int pulseAlpha = p.getPulseAlpha();
        int finalAlpha = Math.min(baseAlpha, pulseAlpha);

        int colorRgb = getFlyColor(p);

        renderTexturedBillboard(matrices, p.x, p.y, p.z, 0.35f, colorRgb, (int) (finalAlpha * 0.6f), cam);
        renderTexturedBillboard(matrices, p.x, p.y, p.z, 0.22f, colorRgb, finalAlpha, cam);
        renderTexturedBillboard(matrices, p.x, p.y, p.z, 0.10f, 0xFFFFFF, finalAlpha, cam);
    }

    private void renderTexturedBillboard(MatrixStack matrices, double wx, double wy, double wz, float size, int colorRgb, int alpha, Vec3d cam) {
        if (alpha <= 0) return;

        matrices.push();
        matrices.translate(wx - cam.x, wy - cam.y, wz - cam.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

        Matrix4f mat = matrices.peek().getPositionMatrix();

        int r = (colorRgb >> 16) & 0xFF;
        int g = (colorRgb >> 8) & 0xFF;
        int b = colorRgb & 0xFF;
        int a = MathHelper.clamp(alpha, 0, 255);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(mat, -size, -size, 0).texture(0, 0).color(r, g, b, a);
        buffer.vertex(mat, -size, size, 0).texture(0, 1).color(r, g, b, a);
        buffer.vertex(mat, size, size, 0).texture(1, 1).color(r, g, b, a);
        buffer.vertex(mat, size, -size, 0).texture(1, 0).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
    }

    private int getFlyColor(FlyEntity p) {
        if (randomColor.getValue()) {
            return p.color;
        }
        return ThemeManager.getInstance().getPrimary();
    }

    private int getAlpha(FlyEntity p) {
        long elapsed = p.timer.getElapsedTime();
        long fadeTime = 500;
        long fadeOutStart = 8000 - fadeTime;

        if (elapsed < fadeTime) {
            return (int) (255.0 * easeOutQuad((float) elapsed / fadeTime));
        } else if (elapsed > fadeOutStart) {
            float progress = Math.min(1.0f, (float) (elapsed - fadeOutStart) / fadeTime);
            return (int) (255.0 * (1.0 - easeOutQuad(progress)));
        }
        return 255;
    }

    private float easeOutQuad(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private class FlyEntity {
        double x, y, z;
        double vx, vy, vz;
        final int color;
        final Timer timer = new Timer();
        final List<double[]> trail = new ArrayList<>();

        FlyEntity(double x, double y, double z, double vx, double vy, double vz, int color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.color = color;
            this.timer.reset();
            this.trail.add(new double[]{x, y, z});
        }

        void update() {
            double randomness = 0.01;
            vx += (Math.random() - 0.5) * randomness;
            vy += (Math.random() - 0.5) * randomness;
            vz += (Math.random() - 0.5) * randomness;

            double maxSpeed = speed.getValue() * 1.5;
            vx = MathHelper.clamp(vx, -maxSpeed, maxSpeed);
            vy = MathHelper.clamp(vy, -maxSpeed, maxSpeed);
            vz = MathHelper.clamp(vz, -maxSpeed, maxSpeed);

            x += vx;
            y += vy;
            z += vz;

            trail.add(new double[]{x, y, z});
            int maxTrail = trailLength.getIntValue();
            while (trail.size() > maxTrail) {
                trail.remove(0);
            }
        }

        double getDistance(double px, double py, double pz) {
            double dx = x - px;
            double dy = y - py;
            double dz = z - pz;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        int getPulseAlpha() {
            double pulse = (Math.sin(timer.getElapsedTime() / 300.0) + 1.0) / 2.0;
            return (int) (pulse * 255);
        }
    }
}

