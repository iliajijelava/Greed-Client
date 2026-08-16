package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.*;

@ModuleInformation(moduleName = "Box Esp", moduleCategory = ModuleCategory.RENDER)
public class BoxEsp extends Module {
    private SliderSetting alpha = new SliderSetting("Alpha", this, 120, 0, 255, 1);

    public BoxEsp() {
        addSetting(alpha);
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer e) {
        if (mc.player == null || mc.world == null) return;

        float pt = e.getRenderTickCounter().getTickDelta(false);
        float a = alpha.getFloatValue() / 255f;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            renderBox(e, player, pt, a);
        }
    }

    private void renderBox(EventWorldRenderer e, PlayerEntity player, float pt, float a) {
        double x = MathHelper.lerp(pt, player.prevX, player.getX());
        double y = MathHelper.lerp(pt, player.prevY, player.getY());
        double z = MathHelper.lerp(pt, player.prevZ, player.getZ());

        float hw = player.getWidth() / 2f;
        float h = player.getHeight();

        MatrixStack ms = new MatrixStack();
        ms.push();
        net.minecraft.util.math.Vec3d cam = e.getCamera().getPos();
        ms.translate(x - cam.x, y - cam.y, z - cam.z);

        boolean isFriend = Cheap.getInstance().getFriendManager().contains(player.getName().getString());
        Color boxColor = isFriend ? new Color(100, 255, 100) : new Color(ThemeManager.getInstance().getPrimary());
        float r = boxColor.getRed() / 255f;
        float g = boxColor.getGreen() / 255f;
        float b = boxColor.getBlue() / 255f;

        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer;

        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrix, -hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, 0,  hw).color(r, g, b, a);
        buffer.vertex(matrix, -hw, 0,  hw).color(r, g, b, a);
        
        buffer.vertex(matrix, -hw, h, -hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, h, -hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, h,  hw).color(r, g, b, a);
        buffer.vertex(matrix, -hw, h,  hw).color(r, g, b, a);
        
        buffer.vertex(matrix, -hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, h, -hw).color(r, g, b, a);
        buffer.vertex(matrix, -hw, h, -hw).color(r, g, b, a);
        
        buffer.vertex(matrix, -hw, 0,  hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, 0,  hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, h,  hw).color(r, g, b, a);
        buffer.vertex(matrix, -hw, h,  hw).color(r, g, b, a);
        
        buffer.vertex(matrix, -hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(matrix, -hw, 0,  hw).color(r, g, b, a);
        buffer.vertex(matrix, -hw, h,  hw).color(r, g, b, a);
        buffer.vertex(matrix, -hw, h, -hw).color(r, g, b, a);
        
        buffer.vertex(matrix,  hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, 0,  hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, h,  hw).color(r, g, b, a);
        buffer.vertex(matrix,  hw, h, -hw).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        
        RenderSystem.enableDepthTest();
        RenderSystem.lineWidth(1.5f);
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrix, -hw, 0, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, 0, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, 0, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, 0,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, 0,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix, -hw, 0,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix, -hw, 0,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix, -hw, 0, -hw).color(r, g, b, 1f);
        
        buffer.vertex(matrix, -hw, h, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, h, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, h, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, h,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, h,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix, -hw, h,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix, -hw, h,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix, -hw, h, -hw).color(r, g, b, 1f);
        
        buffer.vertex(matrix, -hw, 0, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix, -hw, h, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, 0, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, h, -hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, 0,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix,  hw, h,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix, -hw, 0,  hw).color(r, g, b, 1f);
        buffer.vertex(matrix, -hw, h,  hw).color(r, g, b, 1f);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        ms.pop();
    }
}

