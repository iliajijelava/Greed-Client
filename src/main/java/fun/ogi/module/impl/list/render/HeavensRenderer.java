package fun.ogi.module.impl.list.render;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.theme.ThemeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class HeavensRenderer {
    private static final Identifier GLOW_TEXTURE = Identifier.of("cheap", "textures/bloom.png");

   public static void renderAll(EventWorldRenderer event, List<Tile> tiles, Heavens module) {
      if (tiles.isEmpty()) return;

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) return;

      long now = System.currentTimeMillis();

      int primary = ThemeManager.getInstance().getPrimary();
      int r = (primary >> 16) & 0xFF;
      int g = (primary >> 8) & 0xFF;
      int b = primary & 0xFF;

      MatrixStack ms = event.getMatrices();
      net.minecraft.util.math.Vec3d cam = event.getCamera().getPos();

      for (Tile tile : tiles) {
         tile.update(now);
         float alpha = tile.getAlpha();
         if (alpha <= 0.001f) continue;

         float worldX = (float) (tile.x - cam.x);
         float worldY = (float) (tile.y - cam.y);
         float worldZ = (float) (tile.z - cam.z);

         int tileA = (int) (alpha * module.tileAlpha.getFloatValue() * 255);
         int pillarA = (int) (alpha * module.pillarAlpha.getFloatValue() * 255);

         float thickness = module.tileThickness.getFloatValue();
         float currentHeight = module.pillarHeight.getFloatValue() * alpha;

         
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

         ms.push();
         ms.translate(worldX, worldY, worldZ);
         drawFlatTile(ms, module.tileSize.getFloatValue(), thickness, r, g, b, tileA);
         ms.pop();

         
         ms.push();
         ms.translate(worldX, worldY + thickness, worldZ);
         drawPillar(ms, module.pillarWidth.getFloatValue(), currentHeight, r, g, b, pillarA);
         ms.pop();

         
         RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, GLOW_TEXTURE);

         ms.push();
         ms.translate(worldX, worldY + thickness + currentHeight / 2.0f, worldZ);
         ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
         ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

         float intensity = module.glowIntensity.getFloatValue();
         float pWidth = module.pillarWidth.getFloatValue();
         float glowSize = pWidth * 8.0f * intensity;
         float glowSize2 = pWidth * 3.0f * intensity;

         drawGlow(ms, r, g, b, (int) (100 * alpha * intensity), glowSize);
         drawGlow(ms, r, g, b, (int) (180 * alpha * intensity), glowSize2);
         ms.pop();

         RenderSystem.defaultBlendFunc();
      }

      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
   }

    private static void drawFlatTile(MatrixStack ms, float size, float thickness, int r, int g, int b, int a) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float half = size / 2.0f;
        float th = thickness / 2.0f;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        
        buffer.vertex(m, -half, th, -half).color(r, g, b, a);
        buffer.vertex(m, -half, th, half).color(r, g, b, a);
        buffer.vertex(m, half, th, half).color(r, g, b, a);
        buffer.vertex(m, half, th, -half).color(r, g, b, a);

        
        buffer.vertex(m, -half, -th, -half).color(r, g, b, a);
        buffer.vertex(m, half, -th, -half).color(r, g, b, a);
        buffer.vertex(m, half, -th, half).color(r, g, b, a);
        buffer.vertex(m, -half, -th, half).color(r, g, b, a);

        
        buffer.vertex(m, -half, -th, -half).color(r, g, b, a);
        buffer.vertex(m, -half, -th, half).color(r, g, b, a);
        buffer.vertex(m, -half, th, half).color(r, g, b, a);
        buffer.vertex(m, -half, th, -half).color(r, g, b, a);

        
        buffer.vertex(m, half, -th, -half).color(r, g, b, a);
        buffer.vertex(m, half, th, -half).color(r, g, b, a);
        buffer.vertex(m, half, th, half).color(r, g, b, a);
        buffer.vertex(m, half, -th, half).color(r, g, b, a);

        
        buffer.vertex(m, -half, -th, -half).color(r, g, b, a);
        buffer.vertex(m, -half, th, -half).color(r, g, b, a);
        buffer.vertex(m, half, th, -half).color(r, g, b, a);
        buffer.vertex(m, half, -th, -half).color(r, g, b, a);

        
        buffer.vertex(m, -half, -th, half).color(r, g, b, a);
        buffer.vertex(m, half, -th, half).color(r, g, b, a);
        buffer.vertex(m, half, th, half).color(r, g, b, a);
        buffer.vertex(m, -half, th, half).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawPillar(MatrixStack ms, float width, float height, int r, int g, int b, int a) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float hw = width / 2.0f;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        
        buffer.vertex(m, -hw, height, -hw).color(r, g, b, a);
        buffer.vertex(m, -hw, height, hw).color(r, g, b, a);
        buffer.vertex(m, hw, height, hw).color(r, g, b, a);
        buffer.vertex(m, hw, height, -hw).color(r, g, b, a);

        
        buffer.vertex(m, -hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(m, hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(m, hw, 0, hw).color(r, g, b, a);
        buffer.vertex(m, -hw, 0, hw).color(r, g, b, a);

        
        buffer.vertex(m, -hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(m, -hw, 0, hw).color(r, g, b, a);
        buffer.vertex(m, -hw, height, hw).color(r, g, b, a);
        buffer.vertex(m, -hw, height, -hw).color(r, g, b, a);

        
        buffer.vertex(m, hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(m, hw, height, -hw).color(r, g, b, a);
        buffer.vertex(m, hw, height, hw).color(r, g, b, a);
        buffer.vertex(m, hw, 0, hw).color(r, g, b, a);

        
        buffer.vertex(m, -hw, 0, -hw).color(r, g, b, a);
        buffer.vertex(m, -hw, height, -hw).color(r, g, b, a);
        buffer.vertex(m, hw, height, -hw).color(r, g, b, a);
        buffer.vertex(m, hw, 0, -hw).color(r, g, b, a);

        
        buffer.vertex(m, -hw, 0, hw).color(r, g, b, a);
        buffer.vertex(m, hw, 0, hw).color(r, g, b, a);
        buffer.vertex(m, hw, height, hw).color(r, g, b, a);
        buffer.vertex(m, -hw, height, hw).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void drawGlow(MatrixStack ms, int r, int g, int b, int a, float size) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float half = size / 2.0f;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buffer.vertex(m, -half, -half, 0).texture(0, 1).color(r, g, b, a);
        buffer.vertex(m, -half, half, 0).texture(0, 0).color(r, g, b, a);
        buffer.vertex(m, half, half, 0).texture(1, 0).color(r, g, b, a);
        buffer.vertex(m, half, -half, 0).texture(1, 1).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

   public static class Tile {
      public final double x, y, z;
      private final long startTime;
      public final int lifetimeMs;
      private final int animSpeed;
      private float alpha;

      public Tile(double x, double y, double z, int animSpeed, int lifetimeMs) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.startTime = System.currentTimeMillis();
         this.animSpeed = Math.max(animSpeed, 1);
         this.lifetimeMs = lifetimeMs;
         this.alpha = 0.0f;
      }

      public void update(long now) {
         float elapsed = (float) (now - startTime);
         float t = Math.min(1.0f, elapsed / animSpeed);
         if (t < 0.5f) {
            alpha = 2.0f * t * t;
         } else {
            alpha = 2.0f * (1.0f - t) * (1.0f - t);
         }
      }

      public float getAlpha() {
         return alpha;
      }

      public boolean isExpired(long now) {
         return (now - startTime) > lifetimeMs;
      }
   }
}

