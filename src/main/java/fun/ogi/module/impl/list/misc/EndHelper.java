package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInformation(moduleName = "End Helper", moduleDesc = "Helps in end dang", moduleCategory = ModuleCategory.MISC)
public class EndHelper extends Module {
    private static final Box FULL_BOX = new Box(0, 0, 0, 1, 1, 1);
    private final SliderSetting range = new SliderSetting("Range", this, 30, 5, 60, 1);
    private final Set<BlockPos> cachedBlockEntities = ConcurrentHashMap.newKeySet();
    private int scanCooldown = 0;

    public EndHelper() {
        addSetting(range);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.world == null || mc.player == null) return;

        scanCooldown++;
        if (scanCooldown >= 20) {
            scanCooldown = 0;
            cachedBlockEntities.clear();

            int viewDistance = (int) (range.getValue() / 16) + 1;
            net.minecraft.util.math.ChunkPos playerChunk = mc.player.getChunkPos();

            for (int cx = -viewDistance; cx <= viewDistance; cx++) {
                for (int cz = -viewDistance; cz <= viewDistance; cz++) {
                    int chunkX = playerChunk.x + cx;
                    int chunkZ = playerChunk.z + cz;
                    if (!mc.world.isChunkLoaded(chunkX, chunkZ)) continue;

                    net.minecraft.world.chunk.WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);
                    for (BlockEntity be : chunk.getBlockEntityPositions().stream()
                            .map(chunk::getBlockEntity)
                            .filter(be -> be != null && isValidEntity(be))
                            .toList()) {
                        cachedBlockEntities.add(be.getPos());
                    }
                }
            }
        }
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.world == null || mc.player == null) return;

        double maxDistSq = range.getValue() * range.getValue();


        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        MatrixStack ms = new MatrixStack();
        ms.push();
        Matrix4f matrix = ms.peek().getPositionMatrix();
        net.minecraft.util.math.Vec3d cam = event.getCamera().getPos();


        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (BlockPos pos : cachedBlockEntities) {
            if (mc.player.squaredDistanceTo(pos.toCenterPos()) > maxDistSq) continue;
            BlockEntity be = mc.world.getBlockEntity(pos);
            if (be == null) continue;
            Color color = getBlockColor(be);
            for (Box box : getBoundingBox(pos)) {
                drawVazaOverlay(matrix, buffer, box.offset(-cam.x, -cam.y, -cam.z), color, 50);
            }
        }
        BuiltBuffer built = buffer.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);


        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
    private Color getBlockColor(BlockEntity entity) {
        if (entity instanceof DecoratedPotBlockEntity) return new Color(119, 36, 5);
        if (entity instanceof DecoratedPotBlockEntity) return new Color(119, 36, 5);
        return Color.WHITE;
    }
    private boolean isValidEntity(BlockEntity entity) {
        if (entity instanceof DecoratedPotBlockEntity) return true;
        if (entity instanceof BrushableBlockEntity) return true;
        return false;
    }

    private java.util.List<Box> getBoundingBox(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        VoxelShape shape = blockState.getOutlineShape(mc.world, blockPos);
        if (shape.isEmpty()) return List.of(FULL_BOX.offset(blockPos));
        return shape.getBoundingBoxes().stream().map(box -> box.offset(blockPos)).toList();
    }

    private void drawVazaOverlay(Matrix4f matrix, BufferBuilder buffer, Box box, Color color, int alpha) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = alpha / 255f;
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
    }
}

