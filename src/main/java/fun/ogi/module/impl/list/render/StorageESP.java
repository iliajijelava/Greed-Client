package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.*;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static fun.ogi.util.MinecraftUtil.mc;

@ModuleInformation(moduleName = "StorageESP", moduleDesc = "Highlights storage blocks", moduleCategory = ModuleCategory.RENDER)
public class StorageESP extends Module {

    private static final Box FULL_BOX = new Box(0, 0, 0, 1, 1, 1);

    private final ListSetting blocks = new ListSetting("Blocks", this,
            "Chest", "Shulkers", "Hopper", "Ender Chest", "Droppers", "Furnaces", "Barrels", "Minecart", "Trapped Chests");
    private final ListSetting renderMode = new ListSetting("Render Mode", this, "Fill", "Line", "Outline", "Diagonals");
    private final SliderSetting maxDistance = new SliderSetting("Max distance", this, 128, 5, 128, 1);

    private final Set<BlockPos> cachedBlockEntities = ConcurrentHashMap.newKeySet();
    private int scanCooldown = 0;
    public StorageESP(){
        addSettings(blocks,renderMode,maxDistance);
    }
    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.world == null || mc.player == null) return;

        scanCooldown++;
        if (scanCooldown < 10) return;
        scanCooldown = 0;

        
        cachedBlockEntities.removeIf(pos -> {
            if (!mc.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return true;
            return !isStorageBlock(mc.world.getBlockState(pos).getBlock());
        });

        int viewDistance = (int) (maxDistance.getValue() / 16) + 1;
        ChunkPos playerChunk = mc.player.getChunkPos();

        for (int cx = -viewDistance; cx <= viewDistance; cx++) {
            for (int cz = -viewDistance; cz <= viewDistance; cz++) {
                int chunkX = playerChunk.x + cx;
                int chunkZ = playerChunk.z + cz;
                if (!mc.world.isChunkLoaded(chunkX, chunkZ)) continue;

                WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);

                
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (isValidEntity(be)) cachedBlockEntities.add(be.getPos());
                }
                
                for (BlockPos pos : chunk.getBlockEntityPositions()) {
                    BlockEntity be = chunk.getBlockEntity(pos);
                    if (be != null && isValidEntity(be)) cachedBlockEntities.add(pos);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        cachedBlockEntities.clear();
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.world == null || mc.player == null) return;

        double maxDistSq = maxDistance.getValue() * maxDistance.getValue();

        boolean doFill = renderMode.isSelected("Fill");
        boolean doOutline = renderMode.isSelected("Outline");
        boolean doDiagonals = renderMode.isSelected("Diagonals");
        boolean doLines = renderMode.isSelected("Line");

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        MatrixStack ms = new MatrixStack();
        ms.push();
        Matrix4f matrix = ms.peek().getPositionMatrix();
        Vec3d cam = event.getCamera().getPos();

        
        if (doFill) {
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (BlockPos pos : cachedBlockEntities) {
                if (mc.player.squaredDistanceTo(pos.toCenterPos()) > maxDistSq) continue;
                BlockEntity be = mc.world.getBlockEntity(pos);
                if (be == null) continue;
                Color color = getBlockColor(be);
                for (Box box : getBoundingBox(pos)) {
                    renderFilledBox(matrix, buffer, box.offset(-cam.x, -cam.y, -cam.z), color, 50);
                }
            }
            BuiltBuffer built = buffer.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        }

        if (doOutline || doDiagonals || doLines) {
            BufferBuilder linesBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (BlockPos pos : cachedBlockEntities) {
                if (mc.player.squaredDistanceTo(pos.toCenterPos()) > maxDistSq) continue;
                BlockEntity be = mc.world.getBlockEntity(pos);
                if (be == null) continue;
                Color color = getBlockColor(be);
                for (Box box : getBoundingBox(pos)) {
                    Box rel = box.offset(-cam.x, -cam.y, -cam.z);
                    if (doDiagonals) renderBoxInternalDiagonals(matrix, linesBuffer, rel, color, 100);
                    if (doOutline) renderOutlinedBox(matrix, linesBuffer, rel, color, 100);
                    if (doLines) renderLine(matrix, linesBuffer, pos.toCenterPos().subtract(cam), color, cam);
                }
            }

            
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ChestMinecartEntity && blocks.isSelected("Minecart")
                        && mc.player.squaredDistanceTo(entity.getPos()) <= maxDistSq) {
                    Box box = entity.getBoundingBox().offset(-cam.x, -cam.y, -cam.z);
                    Color color = new Color(255, 200, 100);
                    if (doDiagonals) renderBoxInternalDiagonals(matrix, linesBuffer, box, color, 100);
                    if (doOutline) renderOutlinedBox(matrix, linesBuffer, box, color, 100);
                    if (doLines) renderLine(matrix, linesBuffer, entity.getPos().subtract(cam), color, cam);
                }
            }

            BuiltBuffer built = linesBuffer.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        }

        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private List<Box> getBoundingBox(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        VoxelShape shape = blockState.getOutlineShape(mc.world, blockPos);
        if (shape.isEmpty()) return List.of(FULL_BOX.offset(blockPos));
        return shape.getBoundingBoxes().stream().map(box -> box.offset(blockPos)).toList();
    }

    private boolean isValidEntity(BlockEntity entity) {
        if (entity instanceof ChestBlockEntity && blocks.isSelected("Chest")) return true;
        if (entity instanceof EnderChestBlockEntity && blocks.isSelected("Ender Chest")) return true;
        if (entity instanceof TrappedChestBlockEntity && blocks.isSelected("Trapped Chests")) return true;
        if (entity instanceof FurnaceBlockEntity && blocks.isSelected("Furnaces")) return true;
        if (entity instanceof BarrelBlockEntity && blocks.isSelected("Barrels")) return true;
        if (entity instanceof ShulkerBoxBlockEntity && blocks.isSelected("Shulkers")) return true;
        if (entity instanceof DropperBlockEntity && blocks.isSelected("Droppers")) return true;
        if (entity instanceof DispenserBlockEntity && blocks.isSelected("Droppers")) return true;
        if (entity instanceof HopperBlockEntity && blocks.isSelected("Hopper")) return true;
        return false;
    }

    private boolean isStorageBlock(Block block) {
        if (block == Blocks.CHEST && blocks.isSelected("Chest")) return true;
        if (block == Blocks.TRAPPED_CHEST && blocks.isSelected("Trapped Chests")) return true;
        if (block == Blocks.ENDER_CHEST && blocks.isSelected("Ender Chest")) return true;
        if ((block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER)
                && blocks.isSelected("Furnaces")) return true;
        if (block == Blocks.BARREL && blocks.isSelected("Barrels")) return true;
        if (block instanceof ShulkerBoxBlock && blocks.isSelected("Shulkers")) return true;
        if ((block == Blocks.DROPPER || block == Blocks.DISPENSER) && blocks.isSelected("Droppers")) return true;
        if (block == Blocks.HOPPER && blocks.isSelected("Hopper")) return true;
        return false;
    }

    private Color getBlockColor(BlockEntity entity) {
        if (entity instanceof ChestBlockEntity) return new Color(255, 131, 54);
        if (entity instanceof EnderChestBlockEntity) return new Color(121, 54, 255);
        if (entity instanceof TrappedChestBlockEntity) return new Color(255, 101, 54);
        if (entity instanceof FurnaceBlockEntity) return new Color(126, 126, 126);
        if (entity instanceof BarrelBlockEntity) return new Color(255, 185, 54);
        if (entity instanceof ShulkerBoxBlockEntity) return new Color(181, 54, 255);
        if (entity instanceof DropperBlockEntity || entity instanceof DispenserBlockEntity) return new Color(100, 100, 100);
        if (entity instanceof HopperBlockEntity) return new Color(100, 100, 100);
        return Color.WHITE;
    }

    

    private void renderFilledBox(Matrix4f matrix, BufferBuilder buffer, Box box, Color color, int alpha) {
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

    private void renderOutlinedBox(Matrix4f matrix, BufferBuilder buffer, Box box, Color color, int alpha) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = alpha / 255f;
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
    }

    private void renderBoxInternalDiagonals(Matrix4f matrix, BufferBuilder buffer, Box box, Color color, int alpha) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = alpha / 255f;
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
    }

    private void renderLine(Matrix4f matrix, BufferBuilder buffer, Vec3d target, Color color, Vec3d cam) {
        if (mc.player == null) return;
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        Vec3d eyes = mc.player.getEyePos().subtract(cam);
        buffer.vertex(matrix, (float) eyes.x, (float) eyes.y, (float) eyes.z).color(r, g, b, 1f);
        buffer.vertex(matrix, (float) target.x, (float) target.y, (float) target.z).color(r, g, b, 1f);
    }
}

