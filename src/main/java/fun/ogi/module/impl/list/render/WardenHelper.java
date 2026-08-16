package fun.ogi.module.impl.list.render;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.renderers.impl.BuiltRectangle;
import fun.ogi.util.render.renderers.impl.BuiltText;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.render.Camera;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInformation(moduleName = "Warden helper", moduleDesc = "Help in warden for funtime", moduleCategory = ModuleCategory.MISC)
public class WardenHelper extends Module {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2})\\s*:\\s*(\\d{2})");

    private final Map<BlockPos, ChestData> chests = new ConcurrentHashMap<>();

    private SliderSetting range = new SliderSetting("Range", this, 30, 5, 60, 1);
    private SliderSetting tagSize = new SliderSetting("Tag size", this, 13, 5, 30, 1);

    public WardenHelper() {
        addSettings(range, tagSize);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.world == null || mc.player == null) return;

        if (mc.player.age % 5 == 0) {
            int r = range.getIntValue();
            BlockPos pPos = mc.player.getBlockPos();

            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    for (int y = -5; y <= 5; y++) {
                        BlockPos pos = pPos.add(x, y, z);
                        if (mc.world.getBlockState(pos).getBlock() instanceof ChestBlock) {
                            updateChestTimer(pos);
                        }
                    }
                }
            }
        }

        Vec3d pVec = mc.player.getPos();
        int r = range.getIntValue();
        Iterator<Map.Entry<BlockPos, ChestData>> iter = chests.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<BlockPos, ChestData> entry = iter.next();
            ChestData data = entry.getValue();
            if (pVec.distanceTo(entry.getKey().toCenterPos()) > r + 10
                || (data.hasValidTimer() && data.getRemainingSeconds() <= 0)) {
                iter.remove();
            }
        }
    }

    private void updateChestTimer(BlockPos pos) {
        if (mc.world == null) return;

        Vec3d center = pos.toCenterPos();
        Box searchBox = new Box(center.x - 1, center.y, center.z - 1, center.x + 1, center.y + 4.0, center.z + 1);

        for (Entity entity : mc.world.getOtherEntities(null, searchBox)) {
            String rawText = null;
            if (entity instanceof DisplayEntity.TextDisplayEntity td) {
                rawText = td.getText().getString();
            } else if (entity instanceof ArmorStandEntity as) {
                if (as.getCustomName() != null) rawText = as.getCustomName().getString();
            }

            if (rawText != null) {
                String clean = rawText.replaceAll("(?i)§[0-9A-FK-OR]", "");
                Matcher m = TIME_PATTERN.matcher(clean);
                if (m.find()) {
                    int total = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
                    ChestData existing = chests.get(pos);
                    if (existing == null || Math.abs(existing.timerSeconds - total) > 1) {
                        chests.put(pos, new ChestData(total, entity));
                    }
                    return;
                }
            }
        }
        chests.putIfAbsent(pos, new ChestData(-1, null));
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.world == null || mc.player == null || chests.isEmpty()) return;

        float pt = event.getRenderTickCounter().getTickDelta(false);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        MatrixStack ms = event.getMatrices();

        for (Map.Entry<BlockPos, ChestData> entry : chests.entrySet()) {
            ChestData data = entry.getValue();
            if (!data.hasValidTimer()) continue;
            int left = data.getRemainingSeconds();
            if (left <= 0) continue;

            BlockPos pos = entry.getKey();


        }

        RenderSystem.setShader(
                ShaderProgramKeys.POSITION_COLOR
        );

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }






















































































    private void drawChestLabel(EventWorldRenderer event, BlockPos pos, ChestData data, int remainingSeconds) {
        if (data.entity == null) return;

        float pt = event.getRenderTickCounter().getTickDelta(false);
        Camera camera = mc.gameRenderer.getCamera();

        MatrixStack ms = new MatrixStack();
        ms.push();

        double ex = MathHelper.lerp(pt, data.entity.prevX, data.entity.getX());
        double ey = MathHelper.lerp(pt, data.entity.prevY, data.entity.getY()) + data.entity.getHeight() / 2;
        double ez = MathHelper.lerp(pt, data.entity.prevZ, data.entity.getZ());

        ms.translate(ex, ey, ez);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        float scale = 0.025f;
        ms.scale(-scale, -scale, scale);
        Matrix4f matrix = ms.peek().getPositionMatrix();

        String timeStr = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60);
        float textSize = tagSize.getFloatValue();
        float fullWidth = BIKO_FONT.get().getWidth(timeStr, textSize);
        float fontHeight = BIKO_FONT.get().getMetrics().lineHeight() * textSize;
        float padding = 6;

        float bgW = fullWidth + padding * 2;
        float bgH = fontHeight + padding;
        float bgX = -bgW / 2f;
        float bgY = -bgH / 2f;

        boolean urgent = remainingSeconds <= 30;
        Color txtColor = urgent ? new Color(255, 51, 51) : new Color(51, 255, 51);

        BuiltRectangle bg = Builder.rectangle()
            .size(new SizeState(bgW, bgH))
            .color(new QuadColorState(new Color(new Color(ThemeManager.getInstance().getPalette().getNametagBackground()).getRed(), new Color(ThemeManager.getInstance().getPalette().getNametagBackground()).getGreen(), new Color(ThemeManager.getInstance().getPalette().getNametagBackground()).getBlue(), 255)))
            .radius(new QuadRadiusState(4))
            .build();
        bg.render(matrix, bgX, bgY, 0);

        float textX = -fullWidth / 2f;
        float textY = -fontHeight / 2f + 1;
        BuiltText label = Builder.text()
            .font(BIKO_FONT.get())
            .size(textSize)
            .text(timeStr)
            .thickness(0.02f)
            .color(txtColor.getRGB())
            .build();
        label.render(matrix, textX, textY, 0);

        ms.pop();
    }

    private static class ChestData {
        final int timerSeconds;
        final long startTime;
        final boolean hasTimer;
        final Entity entity;

        ChestData(int seconds, Entity entity) {
            this.timerSeconds = seconds;
            this.hasTimer = seconds > 0;
            this.startTime = System.currentTimeMillis();
            this.entity = entity;
        }

        int getRemainingSeconds() {
            if (!hasTimer) return 0;
            return Math.max(0, timerSeconds - (int) ((System.currentTimeMillis() - startTime) / 1000));
        }

        boolean hasValidTimer() {
            return hasTimer;
        }
    }
}

