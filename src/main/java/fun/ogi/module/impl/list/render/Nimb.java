package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

@ModuleInformation(moduleName = "Nimb", moduleDesc = "Renders a rotating halo above your head", moduleCategory = ModuleCategory.RENDER)
public class Nimb extends Module {

    private static final int NIMBUS_ARMS = 4;
    private static final int NIMBUS_SEGMENTS = 12;
    private static final double NIMBUS_STEP_RADIANS = Math.PI * 0.08;
    private static final double NIMBUS_RADIUS = 0.9;
    private static final float NIMBUS_BASE_SIZE = 0.35f;
    private static final int NIMBUS_MAX_ALPHA = 200;
    private static final int NIMBUS_ALPHA_FALLOFF = 18;
    private static final double NIMBUS_SPEED = 120.0;

    private final Identifier GLOW_TEXTURE = Identifier.of("cheap", "textures/glow.png");
    private final SliderSetting size = new SliderSetting("Size", this, 1.0, 0.5, 2.0, 0.1);
    private final SliderSetting height = new SliderSetting("Height", this, 0.3, -0.5, 1.5, 0.05);
    private final SliderSetting speed = new SliderSetting("Speed", this, 1.0, 0.0, 3.0, 0.1);

    public Nimb() {
        addSettings(size, height, speed);
    }

    @Subscribe
    public void onRender(EventWorldRenderer e) {
        if (mc.player == null || mc.world == null || mc.options.getPerspective().isFirstPerson()) return;

        float tickDelta = e.getRenderTickCounter().getTickDelta(true);
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        double x = MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX());
        double y = MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY()) + mc.player.getHeight() + height.getFloatValue();
        double z = MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ());

        int baseColor = ThemeManager.getInstance().getPrimary();
        long nowMs = System.currentTimeMillis();
        double radPerMs = NIMBUS_SPEED * speed.getFloatValue() * Math.PI / 180.0 / 1000.0;

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);

        MatrixStack ms = e.getMatrices();

        for (int arm = 0; arm < NIMBUS_ARMS; arm++) {
            double baseAngle = radPerMs * nowMs + arm * (Math.PI * 2.0 / NIMBUS_ARMS);
            for (int segment = 0; segment < NIMBUS_SEGMENTS; segment++) {
                double segAngle = baseAngle - segment * NIMBUS_STEP_RADIANS;
                double offsetX = Math.cos(segAngle) * NIMBUS_RADIUS * size.getFloatValue();
                double offsetZ = Math.sin(segAngle) * NIMBUS_RADIUS * size.getFloatValue();

                float progress = segment / (float) Math.max(1, NIMBUS_SEGMENTS - 1);
                float billboardSize = NIMBUS_BASE_SIZE * (1.0f - progress * 0.7f) * size.getFloatValue();
                int alpha = MathHelper.clamp(NIMBUS_MAX_ALPHA - segment * NIMBUS_ALPHA_FALLOFF, 0, NIMBUS_MAX_ALPHA);
                int segColor = (baseColor & 0x00FFFFFF) | (alpha << 24);

                float camYaw = camera.getYaw();
                float camPitch = camera.getPitch();

                ms.push();
                ms.translate(x + offsetX - cameraPos.x, y - cameraPos.y, z + offsetZ - cameraPos.z);
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
                ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));

                int r = (segColor >> 16) & 0xFF;
                int g = (segColor >> 8) & 0xFF;
                int b = segColor & 0xFF;
                int a = (segColor >> 24) & 0xFF;

                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                buffer.vertex(ms.peek().getPositionMatrix(), -billboardSize, -billboardSize, 0).texture(0f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(),  billboardSize, -billboardSize, 0).texture(1f, 1f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(),  billboardSize,  billboardSize, 0).texture(1f, 0f).color(r, g, b, a);
                buffer.vertex(ms.peek().getPositionMatrix(), -billboardSize,  billboardSize, 0).texture(0f, 0f).color(r, g, b, a);
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                ms.pop();
            }
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}

