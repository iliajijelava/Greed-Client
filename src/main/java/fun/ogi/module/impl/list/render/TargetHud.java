package fun.ogi.module.impl.list.render;

import com.google.common.base.Suppliers;
import fun.ogi.Cheap;
import fun.ogi.module.impl.list.combat.AttackAura;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.render.Draggable;
import fun.ogi.util.render.ShadowUtil;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.renderers.impl.BuiltTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TargetHud {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final Draggable draggable = new Draggable(100, 100, 112, 26);
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> MACAN_ICONS_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("macan_icons").data("macan_icons").build());

    private static final float WIDTH = 130.0F;
    private static final float COMPACT_HEIGHT = 36.0F;

    private LivingEntity lastTarget;
    private final Animation panelAnimation = new Animation();
    private final Animation healthAnimation = new Animation();
    private final Animation barAnimation = new Animation();
    private final Animation sizeAnimation = new Animation();

    
    private float lastHealth = -1;
    private long damageTime = 0;
    private boolean isDamaged = false;
    private float damageScale = 1f;
    private float damageRed = 0f;

    public TargetHud() {
        panelAnimation.setValue(0f);
        healthAnimation.setValue(0f);
        barAnimation.setValue(0f);
        sizeAnimation.setValue(0f);
    }

    public void render(DrawContext context, float scaleValue, String style, int mouseX, int mouseY, float screenWidth, float screenHeight) {
        this.render(context, scaleValue, style, mouseX, mouseY, screenWidth, screenHeight, "Default");
    }

    public void render(DrawContext context, float scaleValue, String style, int mouseX, int mouseY, float screenWidth, float screenHeight, String mode) {
        context.getMatrices().push();
        context.getMatrices().scale(scaleValue, scaleValue, 1);

        if (mc.currentScreen instanceof ChatScreen) {
            draggable.onDraw(mouseX, mouseY, screenWidth, screenHeight);
        }

        LivingEntity target = resolveTarget();
        boolean preview = target == null && mc.currentScreen instanceof ChatScreen && mc.player != null;
        if (preview) target = mc.player;
        boolean visible = target != null;

        panelAnimation.update();
        panelAnimation.start(panelAnimation.getValue(), visible ? 1f : 0f, 250, Easing.QUART_OUT);
        float alpha = panelAnimation.getValue();
        if (alpha <= 0.01f && !visible) {
            context.getMatrices().pop();
            return;
        }

        sizeAnimation.update();
        sizeAnimation.start(sizeAnimation.getValue(), visible ? 1f : 0f, 300, Easing.QUART_OUT);
        float sizeAnim = sizeAnimation.getValue();
        if (sizeAnim <= 0.01f) {
            context.getMatrices().pop();
            return;
        }

        if (visible && target != null) lastTarget = target;
        if (!visible && lastTarget != null && lastTarget.isAlive()) target = lastTarget;
        if (target == null) { context.getMatrices().pop(); return; }

        
        if (target != null) {
            float currentHealth = target.getHealth();
            if (lastHealth < 0) lastHealth = currentHealth;
            if (currentHealth < lastHealth - 0.01f) { 
                damageTime = System.currentTimeMillis();
                isDamaged = true;
            }
            lastHealth = currentHealth;

            if (isDamaged) {
                long elapsed = System.currentTimeMillis() - damageTime;
                float progress = Math.min(elapsed / 400f, 1f);
                float factor = (float) Math.sin(progress * Math.PI);
                damageScale = 1f - 0.3f * factor;
                damageRed = factor;
                if (progress >= 1f) {
                    isDamaged = false;
                    damageScale = 1f;
                    damageRed = 0f;
                }
            } else {
                damageScale = 1f;
                damageRed = 0f;
            }
        }

        float cx = draggable.getX() + draggable.getWidth() / 2f;
        float cy = draggable.getY() + COMPACT_HEIGHT / 2f;
        context.getMatrices().translate(cx, cy, 0);
        context.getMatrices().scale(sizeAnim, sizeAnim, 1);
        context.getMatrices().translate(-cx, -cy, 0);

        draggable.setWidth(WIDTH);
        draggable.setHeight(COMPACT_HEIGHT);

        if (mode.equals("Macan")) {
            renderMacanTarget(context, target, alpha);
        } else if (mode.equals("Old")) {
            renderOldTarget(context, target, alpha);
        } else {
            renderTarget(context, target, style, mouseX, mouseY, alpha);
        }
        context.getMatrices().pop();
    }

    public void handleMouse(double mouseX, double mouseY, int button, int action) {
        if (mc.currentScreen instanceof ChatScreen) {
            if (action == 1) {
                draggable.onClick((int) mouseX, (int) mouseY, button);
            } else if (action == 0) {
                draggable.onRelease(button);
            }
        }
    }

    

    private void renderOldTarget(DrawContext context, LivingEntity entity, float alpha) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float mainHeight = 26f;
        float faceSize = 18f;
        float facePad = 4f;
        float rounding = 5f;

        float w = 112f;
        float x = draggable.getX();
        float y = draggable.getY();
        float totalH = mainHeight;

        List<ItemStack> armorItems = getMacanArmorItems(entity);
        float armorH = 0f;
        if (!armorItems.isEmpty()) {
            armorH = 11f + 2f;
            totalH += armorH;
        }

        draggable.setWidth(w);
        draggable.setHeight(totalH);

        
        Color gradientStart = new Color(ThemeManager.getInstance().getPrimary());
        Color gradientEnd = new Color(ThemeManager.getInstance().getSecondary());
        ShadowUtil.gradient(gradientStart, gradientEnd, w, mainHeight, new QuadRadiusState(rounding)).render(matrix, x, y, 0);
        drawGradientBackground(matrix, x, y, w, mainHeight, rounding, gradientStart, gradientEnd);

        
        float faceX = x + facePad;
        float faceY = y + facePad;
        renderOldFace(context, entity, faceX, faceY, faceSize, alpha);

        float contentX = faceX + faceSize + facePad;

        MsdfFont nameFont = BIKO_FONT.get();
        MsdfFont hpTextFont = BIKO_FONT.get();
        MsdfFont hpIconFont = MACAN_ICONS_FONT.get();

        float hp = Math.max(0, Math.min(entity.getHealth(), entity.getMaxHealth()));
        float goldenHp = Math.max(0.0F, entity.getAbsorptionAmount());
        float totalHp = hp + goldenHp;

        String hpString = String.format("%.1f", totalHp);
        float hpIconW = hpIconFont.getWidth("O", 7f);
        float hpTextW = hpTextFont.getWidth(hpString, 6.5f);
        float hpIconX = x + w - facePad - hpIconW;
        float hpTextX = hpIconX - 3f - hpTextW;
        String name = entity.getName().getString();

        
        int whiteColor = new Color(255, 255, 255, (int)(255 * alpha)).getRGB();

        Builder.text().text(name).font(nameFont).size(7f).thickness(0.06f).color(new Color(whiteColor, true))
                .build().render(matrix, contentX, y + 7f);

        Builder.text().text("O").font(hpIconFont).size(7f).thickness(0.06f).color(new Color(whiteColor, true))
                .build().render(matrix, hpIconX, y + 7f);
        Builder.text().text(hpString).font(hpTextFont).size(6.5f).thickness(0.06f).color(new Color(whiteColor, true))
                .build().render(matrix, hpTextX, y + 7.15f);

        
        float barY = y + mainHeight - 8f;  
        float barH = 3f;                   
        float barX = contentX;
        float barW = w - (contentX - x) - facePad;
        float maxHealth = Math.max(1f, entity.getMaxHealth());
        float healthPixels = Math.max(1f, Math.min(hp / maxHealth * barW, barW)); 
        float absorptionPixels = Math.max(0f, Math.min(goldenHp / maxHealth * barW, barW));

        
        Builder.rectangle()
                .size(new SizeState(barW, barH))
                .color(new QuadColorState(new Color(0, 0, 0, (int)(180 * alpha))))
                .build()
                .render(matrix, barX, barY, 0);

        
        Builder.rectangle()
                .size(new SizeState(healthPixels, barH))
                .color(new QuadColorState(new Color(255, 255, 255, (int)(255 * alpha))))
                .build()
                .render(matrix, barX, barY, 0);

        
        if (absorptionPixels > 0.5f) {
            Builder.rectangle()
                    .size(new SizeState(absorptionPixels, barH))
                    .color(new QuadColorState(new Color(255, 215, 0, (int)(255 * alpha))))
                    .build()
                    .render(matrix, barX, barY, 0);
        }

        
        if (!armorItems.isEmpty()) {
            float armorY = y + mainHeight;
            float armorCenterX = x + w / 2f;
            float startX = armorCenterX - armorItems.size() * 5.5f;
            context.getMatrices().push();
            context.getMatrices().translate(startX, armorY + 1f, -200);
            float itemOff = -10.5f;
            for (ItemStack stack : armorItems) {
                context.getMatrices().push();
                context.getMatrices().translate(itemOff += 11, 0.5f, 0);
                context.getMatrices().scale(0.5f, 0.5f, 1);
                context.drawItem(stack, 0, 0);
                context.getMatrices().pop();
            }
            context.getMatrices().pop();
        }
    }

    private void renderOldFace(DrawContext context, LivingEntity entity, float x, float y, float size, float alpha) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        if (entity instanceof AbstractClientPlayerEntity player) {
            AbstractTexture skinTex = mc.getTextureManager().getTexture(player.getSkinTextures().texture());
            if (skinTex != null) {
                
                context.getMatrices().push();
                context.getMatrices().translate(x + size/2, y + size/2, 0);
                context.getMatrices().scale(damageScale, damageScale, 1);
                context.getMatrices().translate(-x - size/2, -y - size/2, 0);

                BuiltTexture face = Builder.texture()
                        .size(new SizeState(size, size))
                        .radius(new QuadRadiusState(3))
                        .texture(8 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, skinTex.getGlId())
                        .build();
                face.render(matrix, x, y);

                BuiltTexture hat = Builder.texture()
                        .size(new SizeState(size, size))
                        .radius(new QuadRadiusState(3))
                        .texture(40 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, skinTex.getGlId())
                        .build();
                hat.render(matrix, x, y);

                context.getMatrices().pop();

                
                if (damageRed > 0.01f) {
                    int redAlpha = (int)(damageRed * 0.5f * 255 * alpha);
                    Builder.rectangle()
                            .size(new SizeState(size, size))
                            .radius(new QuadRadiusState(3))
                            .color(new QuadColorState(new Color(255, 0, 0, redAlpha)))
                            .build()
                            .render(matrix, x, y, 0);
                }
            }
        } else {
            
            String targetName = entity.getName().getString();
            String letter = !targetName.isEmpty() ? targetName.substring(0, 1).toUpperCase() : "?";
            Builder.text().text(letter).font(BIKO_FONT.get()).size(10.0F).thickness(0.02f)
                    .color(new Color(255, 255, 255, Math.round(255.0F * alpha)))
                    .build()
                    .render(matrix, x + (size - BIKO_FONT.get().getWidth(letter, 10.0F)) * 0.5F,
                            y + (size - 10.0F) / 2.0F);
        }
    }

    

    private void drawGradientBackground(Matrix4f matrix, float x, float y, float w, float h,
                                        float radius, Color start, Color end) {
        Builder.rectangle()
                .size(new SizeState(w, h))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(start, end, start, end))
                .build().render(matrix, x, y);
    }

    

    private void renderMacanTarget(DrawContext context, LivingEntity entity, float alpha) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float mainHeight = 26f;
        float faceSize = 18f;
        float facePad = 4f;
        int bgColor = new Color(12, 12, 12, (int)(204 * alpha)).getRGB();

        float w = 112f;
        float x = draggable.getX();
        float y = draggable.getY();
        float totalH = mainHeight;

        List<ItemStack> armorItems = getMacanArmorItems(entity);
        float armorH = 0f;
        if (!armorItems.isEmpty()) {
            armorH = 11f + 2f;
            totalH += armorH;
        }

        draggable.setWidth(w);
        draggable.setHeight(totalH);

        ShadowUtil.dark(w, mainHeight, new QuadRadiusState(5)).render(matrix, x, y, 0);
        renderMacanBg(matrix, x, y, w, mainHeight, 5f, bgColor);
        float faceX = x + facePad;
        float faceY = y + facePad;
        renderMacanFace(context, entity, faceX, faceY, faceSize);

        float contentX = faceX + faceSize + facePad;

        MsdfFont nameFont = BIKO_FONT.get();
        MsdfFont hpTextFont = BIKO_FONT.get();
        MsdfFont hpIconFont = MACAN_ICONS_FONT.get();

        float hp = Math.max(0, Math.min(entity.getHealth(), entity.getMaxHealth()));
        float goldenHp = Math.max(0.0F, entity.getAbsorptionAmount());
        float totalHp = hp + goldenHp;

        String hpString = String.format("%.1f", totalHp);
        float hpIconW = hpIconFont.getWidth("O", 7f);
        float hpTextW = hpTextFont.getWidth(hpString, 6.5f);
        float hpIconX = x + w - facePad - hpIconW;
        float hpTextX = hpIconX - 3f - hpTextW;
        float nameMaxW = hpTextX - contentX - 6f;
        String name = entity.getName().getString();

        int textColor = new Color(255, 255, 255, (int)(255 * alpha)).getRGB();
        Builder.text().text(name).font(nameFont).size(7f).thickness(0.06f).color(new Color(textColor, true))
                .build().render(matrix, contentX, y + 7f);

        Builder.text().text("O").font(hpIconFont).size(7f).thickness(0.06f).color(new Color(textColor, true))
                .build().render(matrix, hpIconX, y + 7f);
        Builder.text().text(hpString).font(hpTextFont).size(6.5f).thickness(0.06f).color(new Color(255, 255, 255, (int)(255 * alpha)))
                .build().render(matrix, hpTextX, y + 7.15f);

        float barY = y + mainHeight - 8f;
        float barH = 2f;
        float barX = contentX;
        float barW = w - (contentX - x) - facePad;
        float maxHealth = Math.max(1f, entity.getMaxHealth());
        float healthPixels = Math.max(2f, Math.min(hp / maxHealth * barW, barW));
        float absorptionPixels = Math.max(0f, Math.min(goldenHp / maxHealth * barW, barW));

        Builder.rectangle().size(new SizeState(barW, barH)).radius(new QuadRadiusState(1))
                .color(new QuadColorState(new Color(0, 0, 0, (int)(128 * alpha))))
                .build().render(matrix, barX, barY, 0);

        int themeColor = ThemeManager.getInstance().getPrimary();
        Color healthColor = new Color(themeColor);
        Builder.rectangle().size(new SizeState(healthPixels, barH)).radius(new QuadRadiusState(1))
                .color(new QuadColorState(new Color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), (int)(255 * alpha))))
                .build().render(matrix, barX, barY, 0);

        if (absorptionPixels > 0.5f) {
            Builder.rectangle().size(new SizeState(absorptionPixels, barH)).radius(new QuadRadiusState(1))
                    .color(new QuadColorState(new Color(255, 215, 0, (int)(255 * alpha))))
                    .build().render(matrix, barX, barY, 0);
        }

        if (!armorItems.isEmpty()) {
            float armorY = y + mainHeight;
            float armorCenterX = x + w / 2f;
            float startX = armorCenterX - armorItems.size() * 5.5f;
            context.getMatrices().push();
            context.getMatrices().translate(startX, armorY + 1f, -200);
            float itemOff = -10.5f;
            for (ItemStack stack : armorItems) {
                context.getMatrices().push();
                context.getMatrices().translate(itemOff += 11, 0.5f, 0);
                context.getMatrices().scale(0.5f, 0.5f, 1);
                context.drawItem(stack, 0, 0);
                context.getMatrices().pop();
            }
            context.getMatrices().pop();
        }
    }

    private void renderMacanBg(Matrix4f matrix, float x, float y, float w, float h, float round, int color) {
        Builder.blur().size(new SizeState(w, h)).radius(new QuadRadiusState(round))
                .color(new QuadColorState(new Color(color, true)))
                .blurRadius(10).smoothness(1f).build().render(matrix, x, y);
    }

    private void renderMacanFace(DrawContext context, LivingEntity entity, float x, float y, float size) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            AbstractTexture skinTex = mc.getTextureManager().getTexture(player.getSkinTextures().texture());
            if (skinTex != null) {
                BuiltTexture face = Builder.texture()
                        .size(new SizeState(size, size))
                        .radius(new QuadRadiusState(3))
                        .texture(8 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, skinTex.getGlId())
                        .build();
                face.render(context.getMatrices().peek().getPositionMatrix(), x, y);
                BuiltTexture hat = Builder.texture()
                        .size(new SizeState(size, size))
                        .radius(new QuadRadiusState(3))
                        .texture(40 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, skinTex.getGlId())
                        .build();
                hat.render(context.getMatrices().peek().getPositionMatrix(), x, y);
            }
        }
    }

    private List<ItemStack> getMacanArmorItems(LivingEntity entity) {
        List<ItemStack> items = new ArrayList<>();
        if (!(entity instanceof PlayerEntity)) return items;
        PlayerEntity player = (PlayerEntity) entity;
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty()) items.add(mainHand);
        addIfPresent(items, player, EquipmentSlot.FEET);
        addIfPresent(items, player, EquipmentSlot.LEGS);
        addIfPresent(items, player, EquipmentSlot.CHEST);
        addIfPresent(items, player, EquipmentSlot.HEAD);
        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty()) items.add(offHand);
        return items;
    }

    private void addIfPresent(List<ItemStack> items, PlayerEntity player, EquipmentSlot slot) {
        ItemStack stack = player.getEquippedStack(slot);
        if (!stack.isEmpty()) items.add(stack);
    }

    

    private void renderTarget(DrawContext context, LivingEntity entity, String style, int mouseX, int mouseY, float alpha) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float x = draggable.getX();
        float y = draggable.getY();
        float w = WIDTH;
        float h = COMPACT_HEIGHT;

        Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
        hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), (int)(230 * alpha));

        if (style.equals("Liquid Glass")) {
            Builder.liquid()
                    .size(new SizeState(w, h))
                    .radius(new QuadRadiusState(6, 6, 6, 6))
                    .color(new QuadColorState(new Color(255, 255, 255, 255)))
                    .build()
                    .render(matrix, x, y);
        }else if (style.equals("Colored Liquid")) {
            Builder.coloredLiquid()
                    .size(new SizeState(w, h))
                    .radius(new QuadRadiusState(6, 6, 6, 6))
                    .color(new QuadColorState(new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), (int)(150 * alpha))))
                    .build()
                    .render(matrix, x, y);
        }else if (style.equals("Default")){
            ShadowUtil.dark(w, h, new QuadRadiusState(4)).render(matrix, x, y, 0);
            Builder.rectangle().size(new SizeState(w,h)).radius(new QuadRadiusState(4)).color(new QuadColorState(new Color(20, 20, 20,255))).build().render(matrix,x,y);
        }
        else {
            Builder.blur()
                    .size(new SizeState(w, h))
                    .color(new QuadColorState(hudBg))
                    .radius(new QuadRadiusState(6))
                    .smoothness(1f)
                    .blurRadius(10)
                    .build()
                    .render(matrix, x, y);
        }

        float faceSize = 24f;
        float faceX = x + 5f;
        float faceY = y + (h - faceSize) / 2f;
        renderFace(entity, faceX, faceY, faceSize, alpha, matrix);

        float contentX = x + 33f;
        float nameSize = 7.0F;
        float nameY = y + 4f;

        float targetHealth = entity.getHealth();
        float maxHealth = Math.max(1.0F, entity.getMaxHealth());
        float targetProgress = Math.min(1.0F, targetHealth / maxHealth);

        healthAnimation.update();
        barAnimation.update();
        healthAnimation.start(healthAnimation.getValue(), targetProgress, 200, Easing.QUART_OUT);
        barAnimation.start(barAnimation.getValue(), targetProgress, 200, Easing.QUART_OUT);

        float animProgress = Math.min(1.0F, barAnimation.getValue());

        String hp = Math.round(targetHealth) + "Hp";
        float hpRight = x + w - 7.0F;
        float hpCellWidth = 3.8F;
        int hpColor = new Color(255, 255, 255, Math.round(255.0F * alpha)).getRGB();

        renderStableTextRight(matrix, hp, hpRight, y + 7F, 6F, hpCellWidth, hpColor);

        String name = entity.getName().getString();
        float nameRight = hpRight - hp.length() * hpCellWidth - 3.0F;
        float nameClipWidth = Math.max(0.0F, nameRight - contentX);
        if (nameClipWidth > 0.5F) {
            int nameColor = new Color(255, 255, 255, Math.round(255.0F * alpha)).getRGB();
            Builder.text().text(name).font(BIKO_FONT.get()).size(nameSize).thickness(0.02f).color(nameColor)
                    .build().render(matrix, contentX, nameY);
        }

        float barX = contentX;
        float barWidth = w - 33f - 6f;
        float barHeight = 3F;

        float barY = y + (COMPACT_HEIGHT - 8f);
        Builder.rectangle()
                .size(new SizeState(barWidth, barHeight))
                .color(new QuadColorState(new Color(30, 30, 35, (int)(180 * alpha))))
                .radius(new QuadRadiusState(1))
                .build()
                .render(matrix, barX, barY, 0);

        if (animProgress > 0.01F) {
            int healthColor = ThemeManager.getInstance().getPalette().getTargetHudHealth();
            Color hCol = new Color(healthColor, true);
            int barLeft = new Color(hCol.getRed(), hCol.getGreen(), hCol.getBlue(), (int)(245 * alpha)).getRGB();
            Builder.rectangle()
                    .size(new SizeState(barWidth * Math.min(1.0F, animProgress), barHeight))
                    .color(new QuadColorState(new Color(barLeft)))
                    .radius(new QuadRadiusState(1))
                    .build()
                    .render(matrix, barX, barY, 0);
        }
    }

    private void renderFace(LivingEntity target, float x, float y, float size, float alpha, Matrix4f matrix) {
        if (target instanceof AbstractClientPlayerEntity player) {
            AbstractTexture skinTex = mc.getTextureManager().getTexture(player.getSkinTextures().texture());
            if (skinTex != null) {
                BuiltTexture face = Builder.texture()
                        .size(new SizeState(size, size))
                        .radius(new QuadRadiusState(6))
                        .texture(8 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, skinTex.getGlId())
                        .build();
                face.render(matrix, x, y);
                BuiltTexture hat = Builder.texture()
                        .size(new SizeState(size, size))
                        .radius(new QuadRadiusState(6))
                        .texture(40 / 64f, 8 / 64f, 8 / 64f, 8 / 64f, skinTex.getGlId())
                        .build();
                hat.render(matrix, x, y);
            }
        } else {
            String targetName = target.getName().getString();
            String letter = !targetName.isEmpty() ? targetName.substring(0, 1).toUpperCase() : "?";
            float tw = BIKO_FONT.get().getWidth(letter, 10.0F);
            Builder.text().text(letter).font(BIKO_FONT.get()).size(10.0F).thickness(0.02f)
                    .color(new Color(255, 255, 255, Math.round(255.0F * alpha)))
                    .build()
                    .render(matrix, x + (size - tw) * 0.5F, y + (size - 10.0F) / 2.0F);
        }
    }

    private void renderStableTextRight(Matrix4f matrix, String text, float rightX, float y, float size, float cellWidth, int color) {
        float startX = rightX - text.length() * cellWidth;
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            float characterWidth = BIKO_FONT.get().getWidth(character, size);
            float characterX = startX + i * cellWidth + (cellWidth - characterWidth) * 0.5F;
            Builder.text().text(character).font(BIKO_FONT.get()).size(size).thickness(0.02f).color(color)
                    .build().render(matrix, characterX, y);
        }
    }

    private LivingEntity resolveTarget() {
        AttackAura aura = Cheap.getInstance().getModuleStorage().get(AttackAura.class);
        if (aura != null && aura.isEnabled()) {
            LivingEntity auraTarget = aura.getTarget();
            if (auraTarget != null && auraTarget.isAlive()) {
                return auraTarget;
            }
        }
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
            if (!Cheap.getInstance().getFriendManager().contains(living.getName().getString())) {
                return living;
            }
        }
        return null;
    }
}