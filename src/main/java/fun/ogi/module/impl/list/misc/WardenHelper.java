package fun.ogi.module.impl.list.misc;

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
import fun.ogi.util.NotificationManager;

import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.renderers.impl.BuiltRectangle;
import fun.ogi.util.render.renderers.impl.BuiltText;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import net.minecraft.client.render.Camera;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInformation(
        moduleName = "Warden helper",
        moduleDesc = "Help in warden for funtime",
        moduleCategory = ModuleCategory.MISC
)
public class WardenHelper extends Module {

    private static final Supplier<MsdfFont> BIKO_FONT =
            Suppliers.memoize(() ->
                    MsdfFont.builder()
                            .atlas("biko")
                            .data("biko")
                            .build()
            );

    private static final Pattern TIME_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*:\\s*(\\d{2})");

    private final Map<BlockPos, ChestData> chests =
            new ConcurrentHashMap<>();

    private final Set<BlockPos> notifiedChests =
            ConcurrentHashMap.newKeySet();

    private final SliderSetting range =
            new SliderSetting("Range", this, 30, 5, 60, 1);

    private final SliderSetting tagSize =
            new SliderSetting("Tag size", this, 13, 5, 30, 1);
    private final SliderSetting notificationWhen = new SliderSetting("Notification left when left (sec)",this,20,1,60,1);
    public WardenHelper() {
        addSettings(range, tagSize, notificationWhen);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (ChestData data : chests.values()) {
            if (data.entity != null) {
                data.entity.setInvisible(false);
            }
        }
        notifiedChests.clear();
    }



    @Subscribe
    public void onUpdate(EventUpdate event) {

        if (mc.world == null || mc.player == null)
            return;


        if (mc.player.age % 5 == 0) {

            int r = range.getIntValue();
            BlockPos playerPos = mc.player.getBlockPos();

            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {

                    for (int y = -5; y <= 5; y++) {

                        BlockPos pos = playerPos.add(x, y, z);

                        if (mc.world.getBlockState(pos)
                                .getBlock() instanceof ChestBlock) {

                            updateChestTimer(pos);
                        }
                    }
                }
            }
        }



        Vec3d playerPos = mc.player.getPos();

        int r = range.getIntValue();

        Iterator<Map.Entry<BlockPos, ChestData>> iterator =
                chests.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<BlockPos, ChestData> entry =
                    iterator.next();

            BlockPos pos = entry.getKey();

            ChestData data = entry.getValue();

            double distance =
                    playerPos.distanceTo(
                            pos.toCenterPos()
                    );

            if (distance > r + 10 ||
                    (data.hasValidTimer()
                            && data.getRemainingSeconds() <= 0)) {

                iterator.remove();
                notifiedChests.remove(pos);
            }
        }

        int notifyThreshold = (int) notificationWhen.getValue();

        for (Map.Entry<BlockPos, ChestData> entry : chests.entrySet()) {
            BlockPos pos = entry.getKey();
            ChestData data = entry.getValue();

            if (!data.hasValidTimer()) continue;
            int remaining = data.getRemainingSeconds();
            if (remaining <= 0) continue;

            if (remaining <= notifyThreshold && !notifiedChests.contains(pos)) {
                String timeStr = String.format("%02d:%02d", remaining / 60, remaining % 60);
                NotificationManager.post(
                        "Chest opens in " + timeStr + "!",
                        NotificationManager.TYPE_INFO,
                        4000
                );
                notifiedChests.add(pos);
            }

            if (data.entity != null) {
                data.entity.setInvisible(true);
            }
        }
    }


    private void updateChestTimer(BlockPos pos) {

        if (mc.world == null)
            return;

        Vec3d center = pos.toCenterPos();



        Box searchBox = new Box(
                center.x - 1,
                center.y,
                center.z - 1,

                center.x + 1,
                center.y + 4.0,
                center.z + 1
        );

        for (Entity entity :
                mc.world.getOtherEntities(null, searchBox)) {

            String rawText = null;

            if (entity instanceof DisplayEntity.TextDisplayEntity textDisplay) {

                rawText =
                        textDisplay
                                .getText()
                                .getString();
            }
            else if (entity instanceof ArmorStandEntity armorStand) {

                if (armorStand.getCustomName() != null) {

                    rawText =
                            armorStand
                                    .getCustomName()
                                    .getString();
                }
            }

            if (rawText == null)
                continue;


            String clean =
                    rawText.replaceAll(
                            "(?i)§[0-9A-FK-OR]",
                            ""
                    );

            Matcher matcher =
                    TIME_PATTERN.matcher(clean);

            if (!matcher.find())
                continue;

            int minutes =
                    Integer.parseInt(
                            matcher.group(1)
                    );

            int seconds =
                    Integer.parseInt(
                            matcher.group(2)
                    );

            int totalSeconds =
                    minutes * 60 + seconds;

            ChestData existing =
                    chests.get(pos);


            if (existing == null ||
                    Math.abs(
                            existing.timerSeconds
                                    - totalSeconds
                    ) > 1) {

                chests.put(
                        pos,
                        new ChestData(
                                totalSeconds,
                                entity
                        )
                );
            }

            return;
        }


        chests.putIfAbsent(
                pos,
                new ChestData(
                        -1,
                        null
                )
        );
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {

        if (mc.world == null ||
                mc.player == null ||
                chests.isEmpty()) {

            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        RenderSystem.setShader(
                ShaderProgramKeys.POSITION_COLOR
        );

        MatrixStack ms =
                event.getMatrices();

        Vec3d cam =
                event.getCamera().getPos();

        for (Map.Entry<BlockPos, ChestData> entry :
                chests.entrySet()) {

            BlockPos pos =
                    entry.getKey();

            ChestData data =
                    entry.getValue();

            if (!data.hasValidTimer())
                continue;

            int remainingSeconds =
                    data.getRemainingSeconds();

            if (remainingSeconds <= 0)
                continue;

            drawChestBox(
                    ms,
                    pos,
                    remainingSeconds,
                    cam
            );

            drawChestLabel(
                    event,
                    pos,
                    data,
                    remainingSeconds
            );

            RenderSystem.setShader(
                    ShaderProgramKeys.POSITION_COLOR
            );
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();

        RenderSystem.disableBlend();
    }


    private void drawChestBox(
            MatrixStack ms,
            BlockPos pos,
            int remainingSeconds,
            Vec3d cam
    ) {

        ms.push();

        ms.translate(
                pos.getX() - cam.x,
                pos.getY() - cam.y,
                pos.getZ() - cam.z
        );

        float minX = 0.05f;
        float minY = 0.0f;
        float minZ = 0.05f;

        float maxX = 0.95f;
        float maxY = 0.9f;
        float maxZ = 0.95f;


        Color color =
                getBlockColor(
                        remainingSeconds
                );

        float r =
                color.getRed() / 255.0f;

        float g =
                color.getGreen() / 255.0f;

        float b =
                color.getBlue() / 255.0f;

        float a = 0.35f;

        Matrix4f matrix =
                ms.peek()
                        .getPositionMatrix();

        BufferBuilder buffer =
                Tessellator
                        .getInstance()
                        .begin(
                                VertexFormat.DrawMode.QUADS,
                                VertexFormats.POSITION_COLOR
                        );

        buffer.vertex(
                matrix,
                minX,
                minY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                minY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                maxY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                minX,
                maxY,
                maxZ
        ).color(r, g, b, a);


        buffer.vertex(
                matrix,
                maxX,
                minY,
                minZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                minX,
                minY,
                minZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                minX,
                maxY,
                minZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                maxY,
                minZ
        ).color(r, g, b, a);


        buffer.vertex(
                matrix,
                minX,
                minY,
                minZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                minX,
                minY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                minX,
                maxY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                minX,
                maxY,
                minZ
        ).color(r, g, b, a);



        buffer.vertex(
                matrix,
                maxX,
                minY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                minY,
                minZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                maxY,
                minZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                maxY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                minX,
                maxY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                maxY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                maxY,
                minZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                minX,
                maxY,
                minZ
        ).color(r, g, b, a);



        buffer.vertex(
                matrix,
                minX,
                minY,
                minZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                minY,
                minZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                maxX,
                minY,
                maxZ
        ).color(r, g, b, a);

        buffer.vertex(
                matrix,
                minX,
                minY,
                maxZ
        ).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(
                buffer.end()
        );

        ms.pop();
    }



    private Color getBlockColor(int time) {



        if (time <= 10) {

            return new Color(
                    180,
                    255,
                    0
            );
        }



        if (time <= 30) {

            return new Color(
                    0,
                    255,
                    100
            );
        }


        if (time <= 60) {

            return new Color(
                    255,
                    140,
                    0
            );
        }


        if (time <= 120) {

            return new Color(
                    255,
                    50,
                    50
            );
        }


        return Color.WHITE;
    }


    private void drawChestLabel(
            EventWorldRenderer event,
            BlockPos pos,
            ChestData data,
            int remainingSeconds
    ) {

        if (data.entity == null)
            return;

        float pt =
                event.getRenderTickCounter()
                        .getTickDelta(false);

        Camera camera =
                mc.gameRenderer
                        .getCamera();

        MatrixStack ms =
                new MatrixStack();

        ms.push();

        double ex =
                MathHelper.lerp(
                        pt,
                        data.entity.prevX,
                        data.entity.getX()
                );

        double ey =
                MathHelper.lerp(
                        pt,
                        data.entity.prevY,
                        data.entity.getY()
                )
                        + data.entity.getHeight()
                        / 2.0;

        double ez =
                MathHelper.lerp(
                        pt,
                        data.entity.prevZ,
                        data.entity.getZ()
                );

        Vec3d cam =
                event.getCamera().getPos();

        ms.translate(
                ex - cam.x,
                ey - cam.y,
                ez - cam.z
        );

        ms.multiply(
                RotationAxis.POSITIVE_Y
                        .rotationDegrees(
                                -camera.getYaw()
                        )
        );

        ms.multiply(
                RotationAxis.POSITIVE_X
                        .rotationDegrees(
                                camera.getPitch()
                        )
        );

        float scale =
                0.025f;

        ms.scale(
                -scale,
                -scale,
                scale
        );

        Matrix4f matrix =
                ms.peek()
                        .getPositionMatrix();


        String timeStr =
                String.format(
                        "%02d:%02d",
                        remainingSeconds / 60,
                        remainingSeconds % 60
                );

        float textSize =
                tagSize.getFloatValue();

        float fullWidth =
                BIKO_FONT
                        .get()
                        .getWidth(
                                timeStr,
                                textSize
                        );

        float fontHeight =
                BIKO_FONT
                        .get()
                        .getMetrics()
                        .lineHeight()
                        * textSize;

        float padding = 6;

        float bgW =
                fullWidth
                        + padding * 2;

        float bgH =
                fontHeight
                        + padding;

        float bgX =
                -bgW / 2f;

        float bgY =
                -bgH / 2f;



        Color txtColor =
                getBlockColor(
                        remainingSeconds
                );


        Color themeBackground =
                new Color(
                        ThemeManager
                                .getInstance()
                                .getPalette()
                                .getNametagBackground()
                );

        Color background =
                new Color(
                        themeBackground.getRed(),
                        themeBackground.getGreen(),
                        themeBackground.getBlue(),
                        255
                );

        BuiltRectangle bg =
                Builder.rectangle()

                        .size(
                                new SizeState(
                                        bgW,
                                        bgH
                                )
                        )

                        .color(
                                new QuadColorState(
                                        background
                                )
                        )

                        .radius(
                                new QuadRadiusState(
                                        4
                                )
                        )

                        .build();

        bg.render(
                matrix,
                bgX,
                bgY,
                0
        );


        float textX =
                -fullWidth / 2f;

        float textY =
                -fontHeight / 2f
                        + 1;

        BuiltText label =
                Builder.text()

                        .font(
                                BIKO_FONT.get()
                        )

                        .size(
                                textSize
                        )

                        .text(
                                timeStr
                        )

                        .thickness(
                                0.02f
                        )

                        .color(
                                txtColor.getRGB()
                        )

                        .build();

        label.render(
                matrix,
                textX,
                textY,
                0
        );

        ms.pop();
    }

    private static class ChestData {

        final int timerSeconds;

        final long startTime;

        final boolean hasTimer;

        final Entity entity;

        ChestData(
                int seconds,
                Entity entity
        ) {

            this.timerSeconds =
                    seconds;

            this.hasTimer =
                    seconds > 0;

            this.startTime =
                    System.currentTimeMillis();

            this.entity =
                    entity;
        }

        int getRemainingSeconds() {

            if (!hasTimer)
                return 0;

            return Math.max(
                    0,
                    timerSeconds
                            - (int)
                            (
                                    (
                                            System.currentTimeMillis()
                                                    - startTime
                                    )
                                            / 1000
                            )
            );
        }

        boolean hasValidTimer() {

            return hasTimer;
        }
    }
}