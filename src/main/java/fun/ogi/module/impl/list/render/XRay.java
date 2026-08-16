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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static fun.ogi.util.MinecraftUtil.mc;

@ModuleInformation(moduleName = "XRay", moduleDesc = "Highlights ores through walls", moduleCategory = ModuleCategory.RENDER)
public class XRay extends Module {

    private final Set<BlockPos> cachedBlocks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private final Queue<WorldChunk> pendingChunks = new ArrayDeque<>();
    private final ListSetting elements = new ListSetting("Ores", this,
            "Diamond Ore", "Iron Ore", "Gold Ore", "Ancient Debris", "Lapis Ore", "Redstone Ore", "Coal Ore", "Emerald Ore");

    private static final int VIEW_DISTANCE = 4;
    private static final int CHUNKS_PER_TICK = 2;
    private int scanCooldown = 0;
    public XRay(){
        addSettings(elements);
    }
    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.world == null || mc.player == null) return;

        int budget = CHUNKS_PER_TICK;
        while (budget-- > 0) {
            WorldChunk chunk = pendingChunks.poll();
            if (chunk == null) break;
            scanChunk(chunk);
        }

        scanCooldown++;
        if (scanCooldown >= 20) {
            scanCooldown = 0;
            
            cachedBlocks.removeIf(pos -> !mc.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4));
            
            cachedBlocks.removeIf(pos -> {
                BlockState state = mc.world.getBlockState(pos);
                return state.isAir() || !isBlockEnabled(state.getBlock());
            });
            queueNewChunks();
        }
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.world == null || mc.player == null || cachedBlocks.isEmpty()) return;

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
        double maxDistSq = 999999.0;

        for (BlockPos pos : cachedBlocks) {
            if (mc.player.squaredDistanceTo(pos.toCenterPos()) > maxDistSq) continue;

            Block block = mc.world.getBlockState(pos).getBlock();
            Color color = getBlockColor(block);
            Box box = getBoundingBox(pos).offset(-cam.x, -cam.y, -cam.z);
            renderFilledBox(matrix, buffer, box, color, 30);
        }

        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer != null) BufferRenderer.drawWithGlobalProgram(builtBuffer);

        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    @Override
    public void onEnable() {
        cachedBlocks.clear();
        scannedChunks.clear();
        pendingChunks.clear();
        scanCooldown = 0;
    }

    @Override
    public void onDisable() {
        cachedBlocks.clear();
        scannedChunks.clear();
        pendingChunks.clear();
    }

    private void queueNewChunks() {
        if (mc.world == null || mc.player == null) return;

        ChunkPos playerChunk = mc.player.getChunkPos();
        Set<ChunkPos> loaded = new HashSet<>();

        for (int cx = -VIEW_DISTANCE; cx <= VIEW_DISTANCE; cx++) {
            for (int cz = -VIEW_DISTANCE; cz <= VIEW_DISTANCE; cz++) {
                int chunkX = playerChunk.x + cx;
                int chunkZ = playerChunk.z + cz;

                if (!mc.world.isChunkLoaded(chunkX, chunkZ)) continue;

                ChunkPos cp = new ChunkPos(chunkX, chunkZ);
                loaded.add(cp);
                if (scannedChunks.add(cp)) {
                    pendingChunks.add(mc.world.getChunk(chunkX, chunkZ));
                }
            }
        }

        scannedChunks.retainAll(loaded);
    }

    private void scanChunk(WorldChunk chunk) {
        if (mc.world == null || chunk == null) return;

        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        for (int x = 0; x < 16; x++) {
            for (int y = mc.world.getBottomY(); y < mc.world.getTopYInclusive(); y++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    Block block = chunk.getBlockState(pos).getBlock();
                    if (isBlockEnabled(block)) {
                        cachedBlocks.add(pos);
                    }
                }
            }
        }
    }

    private boolean isBlockEnabled(Block block) {
        if (block == Blocks.DIAMOND_ORE && elements.isSelected("Diamond Ore")) return true;
        if (block == Blocks.IRON_ORE && elements.isSelected("Iron Ore")) return true;
        if (block == Blocks.GOLD_ORE && elements.isSelected("Gold Ore")) return true;
        if (block == Blocks.ANCIENT_DEBRIS && elements.isSelected("Ancient Debris")) return true;
        if (block == Blocks.LAPIS_ORE && elements.isSelected("Lapis Ore")) return true;
        if (block == Blocks.REDSTONE_ORE && elements.isSelected("Redstone Ore")) return true;
        if (block == Blocks.COAL_ORE && elements.isSelected("Coal Ore")) return true;
        if (block == Blocks.EMERALD_ORE && elements.isSelected("Emerald Ore")) return true;
        return false;
    }

    private Color getBlockColor(Block block) {
        if (block == Blocks.DIAMOND_ORE) return new Color(121, 54, 255);
        if (block == Blocks.IRON_ORE) return new Color(216, 175, 145);
        if (block == Blocks.GOLD_ORE) return new Color(255, 215, 0);
        if (block == Blocks.ANCIENT_DEBRIS) return new Color(255, 131, 54);
        if (block == Blocks.LAPIS_ORE) return new Color(0, 71, 179);
        if (block == Blocks.REDSTONE_ORE) return new Color(255, 0, 0);
        if (block == Blocks.COAL_ORE) return new Color(50, 50, 50);
        if (block == Blocks.EMERALD_ORE) return new Color(0, 200, 80);
        return Color.WHITE;
    }

    private Box getBoundingBox(BlockPos blockPos) {
        VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);
        return shape.isEmpty()
                ? new Box(0, 0, 0, 1, 1, 1).offset(blockPos)
                : shape.getBoundingBox().offset(blockPos);
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
}

