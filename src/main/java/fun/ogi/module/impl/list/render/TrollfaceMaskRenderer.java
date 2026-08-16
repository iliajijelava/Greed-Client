package fun.ogi.module.impl.list.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.util.friend.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class TrollfaceMaskRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private static final Identifier[] TEXTURES = {
            Identifier.of("mre", "images/trollface_1.png"),
            Identifier.of("mre", "images/trollface_2.png"),
            Identifier.of("mre", "images/trollface_3.png"),
            Identifier.of("mre", "images/trollface_4.png"),
            Identifier.of("mre", "images/trollface_5.png"),
            Identifier.of("mre", "images/trollface_6.png"),
    };

    private final int[] glIds = new int[TEXTURES.length];

    public TrollfaceMaskRenderer(
            FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> ctx) {
        super(ctx);
        java.util.Arrays.fill(glIds, -1);
    }

    private int getTexId(int idx) {
        if (glIds[idx] != -1) return glIds[idx];
        try {
            NativeImageBackedTexture tex = new NativeImageBackedTexture(
                    NativeImage.read(
                            MinecraftClient.getInstance()
                                    .getResourceManager()
                                    .getResource(TEXTURES[idx])
                                    .get()
                                    .getInputStream()
                    )
            );
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURES[idx], tex);
            glIds[idx] = tex.getGlId();
        } catch (Exception e) {
            e.printStackTrace();
            glIds[idx] = 0;
        }
        return glIds[idx];
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vertexConsumers,
                       int light, PlayerEntityRenderState state,
                       float limbAngle, float limbDistance) {

        TrollfaceMask module = TrollfaceMask.getInstance();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (module == null || !module.isEnabled() || mc.player == null || mc.world == null) return;

        boolean isSelf   = (state.id == mc.player.getId());
        boolean isFriend = false;
        net.minecraft.entity.Entity entity = mc.world.getEntityById(state.id);
        if (entity != null) isFriend = Cheap.getInstance().getFriendManager().getFriends().contains(entity.getName().getString());

        if (module.target.isSelected("Self") && !isSelf) return;
        if (module.target.isSelected("Friends") && !isFriend) return;
        if (isSelf && mc.options.getPerspective().isFirstPerson()) return;

        int texIdx = switch (module.maskType.getValue()) {
            case "Style 2" -> 1;
            case "Style 3" -> 2;
            case "Style 4" -> 3;
            case "Style 5" -> 4;
            case "Style 6" -> 5;
            default        -> 0;
        };

        int glId = getTexId(texIdx);
        if (glId <= 0) return;

        float sz    = module.size.getFloatValue();
        float alpha = module.alpha.getFloatValue();
        int   a     = (int)(alpha * 255f);
        float half  = 0.25f * sz;
        float hh    = 0.25f;

        if (vertexConsumers instanceof VertexConsumerProvider.Immediate imm) {
            imm.draw();
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        ms.push();
        this.getContextModel().head.rotate(ms);

        drawStraps(ms, hh, a);

        ms.translate(0.0f, -0.25f, -0.262f);
        Matrix4f mat = ms.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.colorMask(false, false, false, false);
        BufferBuilder blk = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        blk.vertex(mat, -(half + 0.005f),  (half + 0.005f), -0.001f).color(0, 0, 0, 0);
        blk.vertex(mat,  (half + 0.005f),  (half + 0.005f), -0.001f).color(0, 0, 0, 0);
        blk.vertex(mat,  (half + 0.005f), -(half + 0.005f), -0.001f).color(0, 0, 0, 0);
        blk.vertex(mat, -(half + 0.005f), -(half + 0.005f), -0.001f).color(0, 0, 0, 0);
        BufferRenderer.drawWithGlobalProgram(blk.end());
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, glId);

        BufferBuilder buf = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(mat, -half,  half, 0f).texture(0f, 1f).color(255, 255, 255, a);
        buf.vertex(mat,  half,  half, 0f).texture(1f, 1f).color(255, 255, 255, a);
        buf.vertex(mat,  half, -half, 0f).texture(1f, 0f).color(255, 255, 255, a);
        buf.vertex(mat, -half, -half, 0f).texture(0f, 0f).color(255, 255, 255, a);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        ms.pop();

        ms.push();
        this.getContextModel().head.rotate(ms);


        drawStraps(ms, hh, a, -0.001f);

        ms.pop();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }


    private void drawStraps(MatrixStack ms, float hh, int a) {
        drawStraps(ms, hh, a, 0f);
    }


    private void drawStraps(MatrixStack ms, float hh, int a, float zBias) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float sh = 0.035f;
        Matrix4f mat = ms.peek().getPositionMatrix();

        BufferBuilder buf = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int r = 20, g = 20, b = 20;

        buf.vertex(mat, -hh - 0.002f,  sh, -hh + zBias).color(r, g, b, a);
        buf.vertex(mat, -hh - 0.002f,  sh,  hh + zBias).color(r, g, b, a);
        buf.vertex(mat, -hh - 0.002f, -sh,  hh + zBias).color(r, g, b, a);
        buf.vertex(mat, -hh - 0.002f, -sh, -hh + zBias).color(r, g, b, a);

        buf.vertex(mat,  hh + 0.002f,  sh,  hh + zBias).color(r, g, b, a);
        buf.vertex(mat,  hh + 0.002f,  sh, -hh + zBias).color(r, g, b, a);
        buf.vertex(mat,  hh + 0.002f, -sh, -hh + zBias).color(r, g, b, a);
        buf.vertex(mat,  hh + 0.002f, -sh,  hh + zBias).color(r, g, b, a);

        buf.vertex(mat, -hh,  sh,  hh + 0.002f + zBias).color(r, g, b, a);
        buf.vertex(mat,  hh,  sh,  hh + 0.002f + zBias).color(r, g, b, a);
        buf.vertex(mat,  hh, -sh,  hh + 0.002f + zBias).color(r, g, b, a);
        buf.vertex(mat, -hh, -sh,  hh + 0.002f + zBias).color(r, g, b, a);

        buf.vertex(mat, -hh,  sh, -hh - 0.002f + zBias).color(r, g, b, a);
        buf.vertex(mat,  hh,  sh, -hh - 0.002f + zBias).color(r, g, b, a);
        buf.vertex(mat,  hh, -sh, -hh - 0.002f + zBias).color(r, g, b, a);
        buf.vertex(mat, -hh, -sh, -hh - 0.002f + zBias).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}

