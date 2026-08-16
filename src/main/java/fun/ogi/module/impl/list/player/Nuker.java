package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.rotation.FreeLookComponent;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import fun.ogi.util.server.ServerUtility;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;

import java.awt.*;

@ModuleInformation(moduleName = "Nuker", moduleDesc = "Mines everything around you buddy", moduleCategory = ModuleCategory.PLAYER)
public class Nuker extends Module {

    private final SliderSetting xzDistance = new SliderSetting("Range XZ", this, 4, 2, 6, 1);
    private final SliderSetting yDistance = new SliderSetting("Range Y", this, 5, 2, 6, 1);
    private final BooleanSetting clientLook = new BooleanSetting("Client Look", this, true);
    private boolean isMining;
    public Nuker() {
        addSettings(xzDistance, yDistance, clientLook);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        int radius = range();
        BlockPos minPos = new BlockPos(-71, 77, -15);
        BlockPos maxPos = new BlockPos(-51, 86, 5);
        boolean spawn = ServerUtility.spawn();

        
        for (int y = 0; y < radius * 2; y++) {
            for (int x = 0; x < radius * 2; x++) {
                for (int z = 0; z < radius * 2; z++) {
                    BlockPos offset = new BlockPos((x % 2 == 0 ? -x : x) / 2, (y % 2 == 0 ? -y : y) / 2, (z % 2 == 0 ? -z : z) / 2);
                    BlockPos pos = mc.player.getBlockPos().add(offset);
                    if (isInSpawnArea(pos, minPos, maxPos, spawn) && mc.world.getBlockState(pos).getBlock() == Blocks.DIAMOND_ORE) {
                        float yaw = calcYaw(pos) + (float) (Math.random() * 4.0 - 2.0);
                        float pitch = calcPitch(pos) + (float) (Math.random() * 2.0 - 1.0);
                        applyRotation(yaw, pitch, pos);
                        mc.interactionManager.updateBlockBreakingProgress(pos, getDirection(pos));
                        mc.player.swingHand(Hand.MAIN_HAND);
                        isMining = true;
                        return;
                    }
                }
            }
        }

        
        for (int y = 0; y < (int) yDistance.getValue(); y++) {
            for (int x = 0; x < radius * 2; x++) {
                for (int zx = 0; zx < radius * 2; zx++) {
                    BlockPos offset = new BlockPos((x % 2 == 0 ? -x : x) / 2, y, (zx % 2 == 0 ? -zx : zx) / 2);
                    BlockPos pos = mc.player.getBlockPos().up().add(offset);
                    if (isInSpawnArea(pos, minPos, maxPos, spawn) && mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
                        float yaw = calcYaw(pos) + (float) (Math.random() * 4.0 - 2.0);
                        float pitch = calcPitch(pos) + (float) (Math.random() * 2.0 - 1.0);
                        applyRotation(yaw, pitch, pos);
                        mc.interactionManager.updateBlockBreakingProgress(pos, getDirection(pos));
                        mc.player.swingHand(Hand.MAIN_HAND);
                        isMining = true;
                        return;
                    }
                }
            }
        }
        isMining = false;
    }

    private void applyRotation(float yaw, float pitch, BlockPos pos) {
        if (clientLook.getValue()) {
            FreeLookComponent.setActive(false);
            RotationComponent.update(new Rotation(yaw, pitch), 180.0F, 180.0F, 180.0F, 180.0F, 180, 0, true);
        } else {
            FreeLookComponent.setActive(true);
            RotationComponent.update(new Rotation(yaw, pitch), 180.0F, 180.0F, 180.0F, 180.0F, 180, 0, false);
        }
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.world == null || mc.player == null) return;

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.lineWidth(10.0F);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        MatrixStack ms = new MatrixStack();
        ms.push();
        Matrix4f matrix = ms.peek().getPositionMatrix();
        net.minecraft.util.math.Vec3d cam = event.getCamera().getPos();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int radius = range();
        BlockPos minPos = new BlockPos(-71, 77, -15);
        BlockPos maxPos = new BlockPos(-51, 86, 5);
        boolean spawn = ServerUtility.spawn();

        if (spawn) {
            renderOutlinedBox(matrix, buffer,
                    new Box(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1, maxPos.getY() + 1, maxPos.getZ() + 1).offset(-cam.x, -cam.y, -cam.z),
                    new Color(0, 255, 0, 110));
        }

        for (int y = 0; y < radius * 2; y++) {
            for (int x = 0; x < radius * 2; x++) {
                for (int z = 0; z < radius * 2; z++) {
                    BlockPos additional = new BlockPos((x % 2 == 0 ? -x : x) / 2, (y % 2 == 0 ? -y : y) / 2, (z % 2 == 0 ? -z : z) / 2);
                    BlockPos pos = mc.player.getBlockPos().add(additional);
                    if (isInSpawnArea(pos, minPos, maxPos, spawn) && mc.world.getBlockState(pos).getBlock() == Blocks.DIAMOND_ORE) {
                        Box box = mc.world.getBlockState(pos).getCullingShape().getBoundingBox().offset(pos).offset(-cam.x, -cam.y, -cam.z);
                        renderOutlinedBox(matrix, buffer, box, new Color(0, 255, 0, 250));
                        renderBoxInternalDiagonals(matrix, buffer, box, new Color(0, 255, 0, 250));
                        BuiltBuffer builtBuffer = buffer.endNullable();
                        if (builtBuffer != null) BufferRenderer.drawWithGlobalProgram(builtBuffer);
                        RenderSystem.enableCull();
                        RenderSystem.enableDepthTest();
                        RenderSystem.disableBlend();
                        return;
                    }
                }
            }
        }

        for (int y = 0; y < (int) yDistance.getValue(); y++) {
            for (int x = 0; x < radius * 2; x++) {
                for (int zx = 0; zx < radius * 2; zx++) {
                    BlockPos additional = new BlockPos((x % 2 == 0 ? -x : x) / 2, y, (zx % 2 == 0 ? -zx : zx) / 2);
                    BlockPos pos = mc.player.getBlockPos().up().add(additional);
                    if (isInSpawnArea(pos, minPos, maxPos, spawn) && mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
                        Box box = mc.world.getBlockState(pos).getCullingShape().getBoundingBox().offset(pos).offset(-cam.x, -cam.y, -cam.z);
                        renderOutlinedBox(matrix, buffer, box, new Color(0, 255, 0, 250));
                        renderBoxInternalDiagonals(matrix, buffer, box, new Color(0, 255, 0, 250));
                        BuiltBuffer builtBuffer = buffer.endNullable();
                        if (builtBuffer != null) BufferRenderer.drawWithGlobalProgram(builtBuffer);
                        RenderSystem.enableCull();
                        RenderSystem.enableDepthTest();
                        RenderSystem.disableBlend();
                        return;
                    }
                }
            }
        }

        BuiltBuffer builtBuffer = buffer.endNullable();
        if (builtBuffer != null) BufferRenderer.drawWithGlobalProgram(builtBuffer);

        ms.pop();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public int range() {
        return (int) xzDistance.getValue();
    }

    private boolean isInSpawnArea(BlockPos pos, BlockPos min, BlockPos max, boolean spawn) {
        if (!spawn) return true;
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    private float calcYaw(BlockPos pos) {
        double dx = pos.getX() - mc.player.getX();
        double dz = pos.getZ() - mc.player.getZ();
        return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
    }

    private float calcPitch(BlockPos pos) {
        double dx = pos.getX() - mc.player.getX();
        double dy = pos.getY() - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = pos.getZ() - mc.player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));
    }

    public Direction getDirection(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        if (pos.getY() > eyesPos.y) {
            return mc.world.getBlockState(pos.add(0, -1, 0)).isReplaceable() ? Direction.DOWN : mc.player.getHorizontalFacing().getOpposite();
        } else {
            return !mc.world.getBlockState(pos.add(0, 1, 0)).isReplaceable() ? mc.player.getHorizontalFacing().getOpposite() : Direction.UP;
        }
    }

    private void renderOutlinedBox(Matrix4f matrix, BufferBuilder buffer, Box box, Color color) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;
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

    private void renderBoxInternalDiagonals(Matrix4f matrix, BufferBuilder buffer, Box box, Color color) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;
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
}

