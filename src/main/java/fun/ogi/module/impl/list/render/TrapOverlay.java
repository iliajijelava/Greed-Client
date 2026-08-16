package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import org.joml.Matrix4f;

import java.awt.*;


@ModuleInformation(moduleName = "Trap Overlay", moduleDesc = "Shows trap overlay", moduleCategory = ModuleCategory.RENDER)
public class TrapOverlay extends Module {
    private LivingEntity target;

    public TrapOverlay(){}
    @Subscribe
    public void onEvent(EventWorldRenderer e){
        if(mc.player == null) return;
        if (mc.crosshairTarget instanceof EntityHitResult hit) {
            if (hit.getEntity() instanceof LivingEntity living && living != mc.player && !Cheap.getInstance().getFriendManager().contains(living.getName().getString())) {
                target = living;
            }
        }
        if (target != null && (target.isRemoved() || !target.isAlive() || mc.player.distanceTo(target) > 50 || Cheap.getInstance().getFriendManager().contains(target.getName().getString()))) {
            target = null;
        }
        if(!mc.player.getMainHandStack().isOf(Items.NETHERITE_SCRAP)) return;
        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();
        net.minecraft.util.math.Vec3d cam = e.getCamera().getPos();

        MatrixStack ms = new MatrixStack();
        ms.push();
        ms.translate(pX - cam.x, pY - cam.y, pZ - cam.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        boolean close = target != null && mc.player.distanceTo(target) <= 3;
        int themeColor = close ? ThemeManager.getInstance().getSecondary() : ThemeManager.getInstance().getPrimary();
        Color c = new Color(themeColor);
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;
        float a = 0.25f;

        Matrix4f matrix = ms.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer;

        
        RenderSystem.disableDepthTest();
        buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrix, -1.5f, 0, -1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 0, -1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 0,  1.5f).color(r, g, b, a);
        buffer.vertex(matrix, -1.5f, 0,  1.5f).color(r, g, b, a);
        
        buffer.vertex(matrix, -1.5f, 3, -1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 3, -1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 3,  1.5f).color(r, g, b, a);
        buffer.vertex(matrix, -1.5f, 3,  1.5f).color(r, g, b, a);
        
        buffer.vertex(matrix, -1.5f, 0, -1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 0, -1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 3, -1.5f).color(r, g, b, a);
        buffer.vertex(matrix, -1.5f, 3, -1.5f).color(r, g, b, a);
        
        buffer.vertex(matrix, -1.5f, 0,  1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 0,  1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 3,  1.5f).color(r, g, b, a);
        buffer.vertex(matrix, -1.5f, 3,  1.5f).color(r, g, b, a);
        
        buffer.vertex(matrix, -1.5f, 0, -1.5f).color(r, g, b, a);
        buffer.vertex(matrix, -1.5f, 0,  1.5f).color(r, g, b, a);
        buffer.vertex(matrix, -1.5f, 3,  1.5f).color(r, g, b, a);
        buffer.vertex(matrix, -1.5f, 3, -1.5f).color(r, g, b, a);
        
        buffer.vertex(matrix,  1.5f, 0, -1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 0,  1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 3,  1.5f).color(r, g, b, a);
        buffer.vertex(matrix,  1.5f, 3, -1.5f).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();

        
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, -1.5f, 0, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 0, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 0, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 0, -1.5f).color(r, g, b, 1f);

        buffer.vertex(matrix, -1.5f, 3, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 3, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 3, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 3, -1.5f).color(r, g, b, 1f);

        buffer.vertex(matrix, -1.5f, 0, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 3, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 0, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 3, 1.5f).color(r, g, b, 1f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(1.5f);
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);


        buffer.vertex(matrix, -1.5f, 0, -0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, -0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 0, 0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, 0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -0.5f, 0, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -0.5f, 0, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 0.5f, 0, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 0.5f, 0, 1.5f).color(r, g, b, 1f);


        buffer.vertex(matrix, -1.5f, 3, -0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, -0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 3, 0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, 0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -0.5f, 3, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -0.5f, 3, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 0.5f, 3, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 0.5f, 3, 1.5f).color(r, g, b, 1f);


        buffer.vertex(matrix, -1.5f, 1, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 1, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 2, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 2, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -0.5f, 0, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -0.5f, 3, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 0.5f, 0, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 0.5f, 3, -1.5f).color(r, g, b, 1f);


        buffer.vertex(matrix, -1.5f, 1, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 1, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 2, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 2, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -0.5f, 0, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -0.5f, 3, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 0.5f, 0, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 0.5f, 3, 1.5f).color(r, g, b, 1f);


        buffer.vertex(matrix, -1.5f, 1, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 1, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 2, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 2, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 0, -0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 3, -0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 0, 0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, -1.5f, 3, 0.5f).color(r, g, b, 1f);


        buffer.vertex(matrix, 1.5f, 1, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 1, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 2, -1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 2, 1.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, -0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, -0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 0, 0.5f).color(r, g, b, 1f);
        buffer.vertex(matrix, 1.5f, 3, 0.5f).color(r, g, b, 1f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        ms.pop();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

    }
}

