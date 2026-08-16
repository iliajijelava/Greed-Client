package fun.ogi.module.impl.list.render;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventWorldRenderer;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.renderers.impl.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.w3c.dom.css.Rect;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleInformation(moduleName = "Nametags", moduleDesc = "Renders something on players top", moduleCategory = ModuleCategory.RENDER)
public class Nametags extends Module {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private final SliderSetting size = new SliderSetting("Nametag size", this, 1.0, 0.5, 3.0, 0.1);
    private final ListSetting elements = new ListSetting("Elements", this, "Armor", "First hand", "Off hand","HP Bar");
    private final ModeSetting style = new ModeSetting("Style", this, "Default", "Default", "Blur", "Liquid Glass", "Old");


    private static final float REFERENCE_DISTANCE = 4.0f;

    private static final float MIN_SIZE_FACTOR = 0.6f;

    private static final float MAX_SIZE_FACTOR = 2.0f;

    private static final float ARMOR_MIN_SIZE_FACTOR = 1.0f;

    private Matrix4f lastPositionMatrix;
    private Matrix4f lastProjectionMatrix;


    private static final class ArmorAnchor {
        final Vec3d pos;
        final float sizeFactor;
        ArmorAnchor(Vec3d pos, float sizeFactor) {
            this.pos = pos;
            this.sizeFactor = sizeFactor;
        }
    }

    private final Map<PlayerEntity, ArmorAnchor> armorAnchors = new HashMap<>();

    public Nametags() {
        addSettings(size, style, elements);

        WorldRenderEvents.END.register(ctx -> {
            lastPositionMatrix = new Matrix4f(ctx.positionMatrix());
            lastProjectionMatrix = new Matrix4f(ctx.projectionMatrix());
        });

        HudRenderCallback.EVENT.register(this::renderArmor);
    }


    private float sizeFactor(double distance) {
        return MathHelper.clamp((float) distance / REFERENCE_DISTANCE, MIN_SIZE_FACTOR, MAX_SIZE_FACTOR);
    }

    @Subscribe
    public void onWorldRender(EventWorldRenderer event) {
        if (mc.world == null || mc.player == null) return;
        MatrixStack ms = new MatrixStack();
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = event.getCamera().getPos();
        float partialTicks = event.getRenderTickCounter().getTickDelta(false);
        float scale = size.getFloatValue();
        float textSize = 10 * scale;
        float padding = 6 * scale;

        armorAnchors.clear();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            String name = player.getDisplayName() != null ? player.getDisplayName().getString() : player.getName().getString();
            float health = player.getHealth();
            String healthStr = String.format("%.0f", health);
            String fullText = name + " " + healthStr;
            float fullWidth = BIKO_FONT.get().getWidth(fullText, textSize);
            float nameWidth = BIKO_FONT.get().getWidth(name, textSize);
            float fontHeight = BIKO_FONT.get().getMetrics().lineHeight() * textSize;
            double x = MathHelper.lerp(partialTicks, player.prevX, player.getX());
            double y = MathHelper.lerp(partialTicks, player.prevY, player.getY());
            double z = MathHelper.lerp(partialTicks, player.prevZ, player.getZ());

            double distanceToPlayer = mc.player.getPos().distanceTo(new Vec3d(x, y, z));
            float sizeFactor = sizeFactor(distanceToPlayer);

            float renderScale = 0.025f * scale * sizeFactor / 2;

            armorAnchors.put(player, new ArmorAnchor(new Vec3d(x, y + player.getHeight() + 0.55, z), sizeFactor));

            ms.push();
            ms.translate(x - camPos.x, y + player.getHeight() + 0.5 - camPos.y, z - camPos.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cam.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
            ms.scale(-renderScale, -renderScale, renderScale);
            Matrix4f matrix = ms.peek().getPositionMatrix();

            float headSize = fontHeight + 2 * scale;
            float bgW = fullWidth + padding * 3 + headSize;
            float bgH = fontHeight + padding;
            float bgX = -bgW / 2f;
            float bgY = -bgH / 2f;
            float headX = bgX + padding;
            float headY = bgY + (bgH - headSize) / 2f;
            boolean isFriend = Cheap.getInstance().getFriendManager().contains(player.getName().getString());
            Color bgColor;
            Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
            hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 255);
            if (isFriend) {
                bgColor = new Color(100, 255, 100, 255);
            } else {
                int nm = ThemeManager.getInstance().getPalette().getNametagBackground();
                bgColor = new Color(new Color(nm).getRed(), new Color(nm).getGreen(), new Color(nm).getBlue(), 255);
            }
            if (style.getValueAsString().equals("Default")) {
                BuiltShadow shadow = Builder.shadow().size(new SizeState(bgW, bgH)).radius(new QuadRadiusState(2))
                        .color(new QuadColorState(new Color(0, 0, 0, 35))).softness(4).offset(1, 1).build();
                shadow.render(matrix, bgX, bgY, 0);
                BuiltRectangle bg = Builder.rectangle().size(new SizeState(bgW, bgH)).color(new QuadColorState(bgColor)).radius(new QuadRadiusState(2)).build();
                bg.render(matrix, bgX, bgY, 0);
            } else if (style.getValueAsString().equals("Blur")) {
                BuiltBlur bg = Builder.blur().size(new SizeState(bgW, bgH)).radius(new QuadRadiusState(2))
                        .color(new QuadColorState(hudBg)).blurRadius(10).smoothness(1f).build();
                bg.render(matrix, bgX, bgY, 0);
            } else if (style.getValueAsString().equals("Old")) {
                Color gradientStart = new Color(ThemeManager.getInstance().getPrimary());
                Color gradientEnd = new Color(ThemeManager.getInstance().getSecondary());
                Color shadowStart = new Color(gradientStart.getRed(), gradientStart.getGreen(), gradientStart.getBlue(), 35);
                Color shadowEnd = new Color(gradientEnd.getRed(), gradientEnd.getGreen(), gradientEnd.getBlue(), 35);
                BuiltShadow shadow = Builder.shadow().size(new SizeState(bgW, bgH)).radius(new QuadRadiusState(2))
                        .color(new QuadColorState(shadowStart, shadowEnd, shadowStart, shadowEnd)).softness(4).offset(1, 1).build();
                shadow.render(matrix, bgX, bgY, 0);
                BuiltRectangle bg = Builder.rectangle().size(new SizeState(bgW, bgH)).color(new QuadColorState(gradientStart, gradientEnd, gradientStart, gradientEnd)).radius(new QuadRadiusState(4)).build();
                bg.render(matrix, bgX, bgY, 0);
            } else if (style.getValueAsString().equals("Liquid Glass")) {
                BuiltLiquid bg = Builder.liquid().size(new SizeState(bgW, bgH)).radius(new QuadRadiusState(2))
                        .color(new QuadColorState(new Color(255, 255, 255, 255))).build();
                bg.render(matrix, bgX, bgY, 0);
            }

            if (player instanceof AbstractClientPlayerEntity clientPlayer) {
                AbstractTexture skinTex = mc.getTextureManager().getTexture(clientPlayer.getSkinTextures().texture());
                if (skinTex != null) {
                    int glId = skinTex.getGlId();
                    BuiltTexture face = Builder.texture()
                            .size(new SizeState(headSize, headSize))
                            .texture(8 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, glId)
                            .build();
                    face.render(matrix, headX, headY);
                    BuiltTexture hat = Builder.texture()
                            .size(new SizeState(headSize, headSize))
                            .texture(40 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, glId)
                            .build();
                    hat.render(matrix, headX, headY);
                }
            }

            float textX = headX + headSize + padding;
            float textY = -fontHeight / 2f + 1;

            BuiltText nameText = Builder.text().font(BIKO_FONT.get()).size(textSize).text(name).thickness(0.02f).color(ThemeManager.getInstance().getPalette().getHudText()).build();
            nameText.render(matrix, textX, textY, 0);
            BuiltText hpText = Builder.text().font(BIKO_FONT.get()).size(textSize).text(healthStr).thickness(0.02f).color(ThemeManager.getInstance().getPalette().getNametagHealth()).build();
            hpText.render(matrix, textX + nameWidth + 4, textY, 0);
            ms.pop();
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();

    }

    private void renderArmor(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.world == null || mc.player == null) return;
        if (lastPositionMatrix == null || lastProjectionMatrix == null) return;

        boolean showArmor = elements.isSelected("Armor");
        boolean showFirstHand = elements.isSelected("First hand");
        boolean showOffHand = elements.isSelected("Off hand");
        boolean showHPBar = elements.isSelected("HP Bar");
        if (!showArmor && !showFirstHand && !showOffHand && !showHPBar) return;

        float scale = size.getFloatValue();
        float baseArmorIconSize = 15f * scale;
        float baseGap = 2f * scale;

        for (Map.Entry<PlayerEntity, ArmorAnchor> entry : armorAnchors.entrySet()) {
            PlayerEntity player = entry.getKey();
            if (!player.isAlive()) continue;

            List<ItemStack> armorItems = new ArrayList<>();
            if (showArmor) {
                addIfPresent(armorItems, player, EquipmentSlot.FEET);
                addIfPresent(armorItems, player, EquipmentSlot.LEGS);
                addIfPresent(armorItems, player, EquipmentSlot.CHEST);
                addIfPresent(armorItems, player, EquipmentSlot.HEAD);
            }

            List<ItemStack> handItems = new ArrayList<>();
            if (showFirstHand) addIfPresent(handItems, player, EquipmentSlot.MAINHAND);
            if (showOffHand) addIfPresent(handItems, player, EquipmentSlot.OFFHAND);

            if (armorItems.isEmpty() && handItems.isEmpty() && !showHPBar) continue;

            ArmorAnchor anchor = entry.getValue();
            Vector4f screen = worldToScreen(anchor.pos);
            if (screen == null) continue;

            float sizeFactor = MathHelper.clamp(anchor.sizeFactor, ARMOR_MIN_SIZE_FACTOR, MAX_SIZE_FACTOR);
            float armorIconSize = baseArmorIconSize * sizeFactor / 3;
            float gap = baseGap * sizeFactor;

            List<ItemStack> allItems = new ArrayList<>(handItems);
            allItems.addAll(armorItems);
            float rowW = allItems.size() * armorIconSize + (allItems.size() - 1) * gap;
            float rowX = screen.x - rowW / 2f;
            float rowY = screen.y - armorIconSize - 6f * scale * sizeFactor;

            if (!allItems.isEmpty()) {
                for (ItemStack stack : allItems) {
                    drawItem(context, stack, rowX, rowY, armorIconSize);
                    rowX += armorIconSize + gap;
                }
            }

            if (showHPBar) {
                float barY = allItems.isEmpty()
                        ? screen.y - 6f * scale * sizeFactor - 2.5f * scale
                        : rowY - 2.5f * scale - 3f * scale * sizeFactor;
                drawHPBar(context, player, screen.x, barY, sizeFactor);
            }
        }
    }
    private void drawHPBar(DrawContext context, PlayerEntity player, float x, float y, float sizeFactor) {
        float scale = size.getFloatValue();
        float barWidth = 30f * scale * sizeFactor;
        float barHeight = 2.5f * scale;

        float maxHealth = Math.max(1f, player.getMaxHealth());
        float hp = MathHelper.clamp(player.getHealth(), 0f, maxHealth);
        float ratio = hp / maxHealth;

        float x0 = x - barWidth / 2f;
        float y0 = y - barHeight;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        Builder.rectangle()
                .size(new SizeState(barWidth, barHeight))
                .radius(new QuadRadiusState(1.5f))
                .color(new QuadColorState(new Color(0, 0, 0, 150)))
                .build()
                .render(matrix, x0, y0, 0);

        float fillW = Math.max(0f, barWidth * ratio);
        if (fillW > 0.5f) {
            Builder.rectangle()
                    .size(new SizeState(fillW, barHeight))
                    .radius(new QuadRadiusState(1.5f))
                    .color(new QuadColorState(healthColor(ratio)))
                    .build()
                    .render(matrix, x0, y0, 0);
        }

        float absorption = player.getAbsorptionAmount();
        float absorptionRatio = MathHelper.clamp(absorption / maxHealth, 0f, 1f);
        if (absorptionRatio > 0.01f) {
            float absW = Math.max(0f, barWidth * absorptionRatio);
            Builder.rectangle()
                    .size(new SizeState(absW, barHeight))
                    .radius(new QuadRadiusState(1.5f))
                    .color(new QuadColorState(new Color(255, 215, 0, 220)))
                    .build()
                    .render(matrix, x0, y0, 0);
        }
    }

    private Color healthColor(float ratio) {
        int r = (int) MathHelper.lerp(ratio, 255f, 60f);
        int g = (int) MathHelper.lerp(ratio, 40f, 255f);
        return new Color(r, g, 40, 255);
    }
    private void drawItem(DrawContext context, ItemStack stack, float x, float y, float size) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        float itemScale = size / 16f;
        context.getMatrices().scale(itemScale, itemScale, 1f);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();
    }

    private Vector4f worldToScreen(Vec3d worldPos) {
        if (mc.getWindow() == null) return null;
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();

        Vector4f clip = new Vector4f(
                (float) (worldPos.x - camPos.x),
                (float) (worldPos.y - camPos.y),
                (float) (worldPos.z - camPos.z),
                1.0f
        );

        lastPositionMatrix.transform(clip);
        lastProjectionMatrix.transform(clip);

        if (clip.w < 0.001f) return null;

        clip.x /= clip.w;
        clip.y /= clip.w;
        clip.z /= clip.w;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        clip.x = (clip.x * 0.5f + 0.5f) * screenW;
        clip.y = (1f - (clip.y * 0.5f + 0.5f)) * screenH;

        return clip;
    }

    private void addIfPresent(List<ItemStack> items, PlayerEntity player, EquipmentSlot slot) {
        ItemStack stack = player.getEquippedStack(slot);
        if (!stack.isEmpty()) items.add(stack);
    }
}