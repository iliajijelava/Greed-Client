package fun.ogi.module.impl.list.render;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.ogi.Cheap;
import fun.ogi.command.impl.BindCommand;
import fun.ogi.mixin.CooldownUtil;
import fun.ogi.module.Module;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.animation.Animation;
import fun.ogi.util.animation.Easing;
import fun.ogi.util.render.Draggable;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class YouGameHud {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("nur").data("nur").build());

    private final Animation cooldownsAlpha = new Animation();
    private final Animation potionsAlpha = new Animation();
    private final Animation keybindsAlpha = new Animation();
    private final Animation armorAlpha = new Animation();

    private final Map<String, Animation> cooldownRows = new HashMap<>();
    private final Map<String, Animation> potionRows = new HashMap<>();
    private final Map<String, Animation> keybindRows = new HashMap<>();
    private final Map<String, Animation> keybindSliders = new HashMap<>();
    private final Map<String, Animation> armorRows = new HashMap<>();

    public void renderCooldowns(DrawContext context, Draggable drag) {
        if (mc.player == null) return;
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        List<CooldownUtil.CooldownEntry> entries = CooldownUtil.getActiveCooldownsFull(mc.player.getItemCooldownManager(), mc.player, tickDelta);

        boolean chat = mc.currentScreen instanceof ChatScreen;
        boolean hasAny = !entries.isEmpty();

        cooldownsAlpha.update();
        cooldownsAlpha.start(cooldownsAlpha.getValue(), hasAny || chat ? 1f : 0f, 200, Easing.CUBIC_OUT);
        float alpha = cooldownsAlpha.getValue();
        if (alpha <= 0.01f) return;

        Set<String> keys = new HashSet<>();
        for (CooldownUtil.CooldownEntry e : entries) keys.add(e.name());
        updateRowAnimations(cooldownRows, keys);

        float posX = drag.getX();
        float posY = drag.getY();

        float headerW = 80f;
        float iconPanelWidth = 13f;
        float gap = 1f;

        float maxCooldownWidth = 0f;
        for (CooldownUtil.CooldownEntry e : entries) {
            float w = BIKO_FONT.get().getWidth(e.time(), 6.75f) + BIKO_FONT.get().getWidth(e.name(), 6.75f);
            if (w > maxCooldownWidth) maxCooldownWidth = w;
        }
        float rightPanelWidth = Math.max(maxCooldownWidth + 16f, 50f);
        float totalW = Math.max(headerW, iconPanelWidth + gap + rightPanelWidth);
        float totalH = 14.5f + entries.size() * 13f;
        drag.setWidth(totalW);
        drag.setHeight(totalH);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        header(matrix, posX, posY, headerW, "T", "Cooldowns", alpha);
        posY += 14.5f;
        int accent = ThemeManager.getInstance().getPrimary();

        for (CooldownUtil.CooldownEntry e : entries) {
            Animation anim = cooldownRows.get(e.name());
            float a = anim != null ? anim.getValue() : 1f;
            if (a <= 0.01f) continue;

            float rowY = posY + a * 3f - 3f;
            float rowAlpha = a * alpha;
            float cooldownTextWidth = BIKO_FONT.get().getWidth(e.time(), 6.75f);
            float itemNameWidth = BIKO_FONT.get().getWidth(e.name(), 6.75f);
            float panelWidth = cooldownTextWidth + itemNameWidth + 16f;

            panel(matrix, posX, rowY, iconPanelWidth, 13f, rowAlpha  * 0.55f);

            context.getMatrices().push();
            context.getMatrices().translate(posX + 6.5f, rowY + 6.5f, 0f);
            context.getMatrices().scale(0.5f, 0.5f, 0.5f);
            context.drawItem(e.stack(), -8, -8);
            context.getMatrices().pop();

            panel(matrix, posX + iconPanelWidth + gap, rowY, panelWidth, 13f, rowAlpha);

            float rowCenter = rowY + (13f - 6.5f) / 2f;
            Builder.text().text(e.time()).font(BIKO_FONT.get()).size(6.5f).thickness(0.06f)
                    .color(alpha(Color.WHITE, rowAlpha  * 0.55f)).build().render(matrix, posX + iconPanelWidth + gap + 4f, rowCenter);

            float nameX = posX + iconPanelWidth + gap + panelWidth - itemNameWidth - 4f;
            Builder.text().text(e.name()).font(BIKO_FONT.get()).size(6.5f).thickness(0.06f)
                    .color(alpha(new Color(accent), rowAlpha  * 0.55f)).build().render(matrix, nameX, rowCenter);

            posY += 13f * a;
        }
    }

    public void renderPotions(DrawContext context, Draggable drag) {
        if (mc.player == null) return;
        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());

        boolean chat = mc.currentScreen instanceof ChatScreen;
        boolean hasAny = !effects.isEmpty();

        potionsAlpha.update();
        potionsAlpha.start(potionsAlpha.getValue(), hasAny || chat ? 1f : 0f, 200, Easing.CUBIC_OUT);
        float alpha = potionsAlpha.getValue();
        if (alpha <= 0.01f) return;

        effects.sort(Comparator
                .comparingInt((StatusEffectInstance e) -> e.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL ? 1 : 0)
                .thenComparing(this::getEffectBaseName));

        Set<String> keys = new HashSet<>();
        for (StatusEffectInstance e : effects) keys.add(potionKey(e));
        updateRowAnimations(potionRows, keys);

        float posX = drag.getX();
        float posY = drag.getY();

        float headerW = 55f;
        float iconPanelWidth = 13f;
        float gap = 1f;

        float maxPotionNameWidth = 0f;
        float maxDurationWidth = 0f;
        for (StatusEffectInstance e : effects) {
            float nw = BIKO_FONT.get().getWidth(fullPotionName(e), 6.75f);
            float dw = BIKO_FONT.get().getWidth(formatDuration(e), 6.75f);
            if (nw > maxPotionNameWidth) maxPotionNameWidth = nw;
            if (dw > maxDurationWidth) maxDurationWidth = dw;
        }
        float rightPanelWidth = Math.max(maxPotionNameWidth + maxDurationWidth + 16f, 40f);
        float totalW = Math.max(headerW, iconPanelWidth + gap + rightPanelWidth);
        float totalH = 14.5f + effects.size() * 13f;
        drag.setWidth(totalW);
        drag.setHeight(totalH);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        header(matrix, posX, posY, headerW, "V", "Potions", alpha);
        posY += 14.5f;
        int accent = ThemeManager.getInstance().getPrimary();

        for (StatusEffectInstance e : effects) {
            Animation anim = potionRows.get(potionKey(e));
            float a = anim != null ? anim.getValue() : 1f;
            if (a <= 0.01f) continue;

            float rowY = posY + a * 3f - 3f;
            float rowAlpha = a * alpha;

            String full = fullPotionName(e);
            String dur = formatDuration(e);
            float potionWidth = BIKO_FONT.get().getWidth(full, 6.75f) + BIKO_FONT.get().getWidth(dur, 6.75f) + 16f;

            panel(matrix, posX, rowY, iconPanelWidth, 13f, rowAlpha * 0.55f);

            Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(e.getEffectType());
            if (sprite != null) {
                drawSprite(context.getMatrices(), sprite, posX + 2.5f, rowY + (13f - 8f) / 2f, 8f);
            }

            panel(matrix, posX + iconPanelWidth + gap, rowY, potionWidth, 13f, rowAlpha * 0.55f);

            float rowCenter = rowY + (13f - 6.5f) / 2f;
            boolean negative = e.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL;
            Color nameColor = negative ? new Color(255, 85, 85) : Color.WHITE;
            Builder.text().text(full).font(BIKO_FONT.get()).size(6.5f).thickness(0.06f)
                    .color(alpha(nameColor, rowAlpha * 0.55f)).build().render(matrix, posX + iconPanelWidth + gap + 4f, rowCenter);

            float durW = BIKO_FONT.get().getWidth(dur, 6.75f);
            float durX = posX + iconPanelWidth + gap + potionWidth - durW - 4f;
            Builder.text().text(dur).font(BIKO_FONT.get()).size(6.5f).thickness(0.06f)
                    .color(alpha(new Color(accent), rowAlpha)).build().render(matrix, durX, rowCenter);

            posY += 13f * a;
        }
    }

    public void renderKeybinds(DrawContext context, Draggable drag) {
        List<Module> modules = Cheap.getInstance().getModuleStorage().getModules().stream()
                .filter(m -> m.isEnabled() && m.getKeybind() != 0 && m.getKeybind() != -1)
                .toList();

        boolean chat = mc.currentScreen instanceof ChatScreen;
        boolean hasAny = !modules.isEmpty();

        keybindsAlpha.update();
        keybindsAlpha.start(keybindsAlpha.getValue(), hasAny || chat ? 1f : 0f, 200, Easing.CUBIC_OUT);
        float alpha = keybindsAlpha.getValue();
        if (alpha <= 0.01f) return;

        Set<String> keys = new HashSet<>();
        for (Module m : modules) keys.add(m.getName());
        updateRowAnimations(keybindRows, keys);

        float posX = drag.getX();
        float posY = drag.getY();

        float headerW = 60f;
        float gap = 1f;
        float leftPanelWidth = 28f;

        float maxModuleWidth = 0f;
        for (Module m : modules) {
            float w = BIKO_FONT.get().getWidth(m.getName(), 6.75f);
            if (w > maxModuleWidth) maxModuleWidth = w;
        }
        float rightPanelWidth = maxModuleWidth + 8f;
        float totalW = Math.max(headerW, leftPanelWidth + gap + rightPanelWidth);
        float totalH = 14.5f + modules.size() * 13f;
        drag.setWidth(totalW);
        drag.setHeight(totalH);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        header(matrix, posX, posY, headerW, "F", "Hotkeys", alpha);
        posY += 14.5f;
        int accent = ThemeManager.getInstance().getPrimary();

        for (Module m : modules) {
            Animation anim = keybindRows.get(m.getName());
            float a = anim != null ? anim.getValue() : 1f;
            if (a <= 0.01f) continue;

            float rowY = posY + a * 3f - 3f;
            float rowAlpha = a * alpha;

            String moduleName = m.getName();
            String keyName = BindCommand.keyToName(m.getKeybind());
            float moduleWidth = BIKO_FONT.get().getWidth(moduleName, 6.75f) + 8f;

            panel(matrix, posX, rowY, leftPanelWidth, 13f, rowAlpha * 0.55f);

            float sliderBgWidth = 12f;
            float sliderBgHeight = 7f;
            float sliderPosX = posX + 4f;
            float sliderPosY = rowY + (13f - sliderBgHeight) / 2f;
            Builder.rectangle().size(new SizeState(sliderBgWidth, sliderBgHeight)).radius(new QuadRadiusState(3f))
                    .color(new QuadColorState(alpha(new Color(35, 35, 35), rowAlpha * 0.55f))).build().render(matrix, sliderPosX, sliderPosY);

            float sliderAnim = updateSlider(m);
            float sliderIndicatorSize = 6f;
            float sliderIndicatorX = sliderPosX + 1f + (sliderAnim * (sliderBgWidth - sliderIndicatorSize - 2f));
            float sliderIndicatorY = sliderPosY + (sliderBgHeight - sliderIndicatorSize) / 2f;
            Builder.rectangle().size(new SizeState(sliderIndicatorSize, sliderIndicatorSize)).radius(new QuadRadiusState(2f))
                    .color(new QuadColorState(alpha(new Color(accent), rowAlpha * 0.55f))).build().render(matrix, sliderIndicatorX, sliderIndicatorY);

            float rowCenter = rowY + (13f - 6.5f) / 2f;
            Builder.text().text(keyName).font(BIKO_FONT.get()).size(6.5f).thickness(0.06f)
                    .color(alpha(Color.WHITE, rowAlpha)).build().render(matrix, posX + 18f, rowCenter);

            panel(matrix, posX + leftPanelWidth + gap, rowY, moduleWidth, 13f, rowAlpha  * 0.55f);
            Builder.text().text(moduleName).font(BIKO_FONT.get()).size(6.5f).thickness(0.06f)
                    .color(alpha(Color.WHITE, rowAlpha)).build().render(matrix, posX + leftPanelWidth + gap + 4f, rowCenter);

            posY += 13f * a;
        }
    }

    public void renderArmor(DrawContext context, Draggable drag) {
        if (mc.player == null) return;
        boolean chat = mc.currentScreen instanceof ChatScreen;

        boolean hasAnyArmor = false;
        for (int i = 3; i >= 0; i--) {
            if (!mc.player.getInventory().armor.get(i).isEmpty()) {
                hasAnyArmor = true;
                break;
            }
        }
        if (!hasAnyArmor) {
            hasAnyArmor = !mc.player.getMainHandStack().isEmpty() || !mc.player.getOffHandStack().isEmpty();
        }

        armorAlpha.update();
        armorAlpha.start(armorAlpha.getValue(), hasAnyArmor || chat ? 1f : 0f, 200, Easing.CUBIC_OUT);
        float alpha = armorAlpha.getValue();
        if (alpha <= 0.01f) return;

        List<ArmorRow> rows = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().armor.get(i);
            if (!stack.isEmpty() || chat) {
                boolean placeholder = stack.isEmpty();
                rows.add(new ArmorRow("slot" + i, placeholder ? placeholderForSlot(i) : stack, durabilityText(stack, chat), placeholder));
            }
        }
        ItemStack main = mc.player.getMainHandStack();
        if (!main.isEmpty() || chat) {
            boolean placeholder = main.isEmpty();
            rows.add(new ArmorRow("main", placeholder ? new ItemStack(Items.DIAMOND_SWORD) : main, durabilityText(main, chat), placeholder));
        }
        ItemStack off = mc.player.getOffHandStack();
        if (!off.isEmpty() || chat) {
            boolean placeholder = off.isEmpty();
            rows.add(new ArmorRow("off", placeholder ? new ItemStack(Items.TOTEM_OF_UNDYING) : off, durabilityText(off, chat), placeholder));
        }

        Set<String> keys = new HashSet<>();
        for (ArmorRow r : rows) keys.add(r.key);
        updateRowAnimations(armorRows, keys);

        float posX = drag.getX();
        float posY = drag.getY();

        float headerW = 45f;
        float iconPanelWidth = 13f;
        float gap = 1f;

        float maxRightW = 0f;
        for (ArmorRow r : rows) {
            if (r.durability.isEmpty()) continue;
            float w = BIKO_FONT.get().getWidth(r.durability, 6.75f) + 8f;
            if (w > maxRightW) maxRightW = w;
        }
        float totalW = Math.max(headerW, iconPanelWidth + gap + maxRightW);
        float totalH = 14.5f + rows.size() * 13f;
        drag.setWidth(totalW);
        drag.setHeight(totalH);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        header(matrix, posX, posY, headerW, "L", "Armor", alpha);
        posY += 14.5f;

        for (ArmorRow r : rows) {
            Animation anim = armorRows.get(r.key);
            float a = anim != null ? anim.getValue() : 1f;
            if (a <= 0.01f) continue;

            float rowY = posY + a * 3f - 3f;
            float itemAlpha = r.placeholder ? 0.3f : 1f;
            float rowAlpha = a * alpha * itemAlpha;

            panel(matrix, posX, rowY, iconPanelWidth, 13f, rowAlpha);

            context.getMatrices().push();
            context.getMatrices().translate(posX + 6.5f, rowY + 6.5f, 0f);
            context.getMatrices().scale(0.5f, 0.5f, 0.5f);
            context.drawItem(r.stack, -8, -8);
            context.getMatrices().pop();

            if (!r.durability.isEmpty()) {
                float textWidth = BIKO_FONT.get().getWidth(r.durability, 6.75f) + 8f;
                panel(matrix, posX + iconPanelWidth + gap, rowY, textWidth, 13f, rowAlpha);
                float textX = posX + iconPanelWidth + gap + (textWidth - BIKO_FONT.get().getWidth(r.durability, 6.75f)) / 2f;
                Builder.text().text(r.durability).font(BIKO_FONT.get()).size(6.5f).thickness(0.06f)
                        .color(alpha(Color.WHITE, rowAlpha)).build().render(matrix, textX, rowY + (13f - 6.5f) / 2f);
            }

            posY += 13f * a;
        }
    }

    private void header(Matrix4f matrix, float x, float y, float w, String icon, String title, float alpha) {
        panel(matrix, x, y, w, 13f, alpha);
        int accent = ThemeManager.getInstance().getPrimary();
        Builder.text().text(icon).font(ICON_FONT.get()).size(8f).thickness(0.08f)
                .color(alpha(new Color(accent), alpha)).build().render(matrix, x + 4f, y + (13f - 8f) / 2f);
        Builder.text().text(title).font(BIKO_FONT.get()).size(7f).thickness(0.06f)
                .color(alpha(Color.WHITE, alpha)).build().render(matrix, x + 16f, y + (13f - 7f) / 2f);
    }

    private void panel(Matrix4f matrix, float x, float y, float w, float h, float alpha) {
        float a = Math.max(0f, Math.min(1f, alpha));
        Builder.rectangle().size(new SizeState(w, h)).radius(new QuadRadiusState(3f))
                .color(new QuadColorState(new Color(20, 20, 20, (int) (255 * a )))).build().render(matrix, x, y);
        Builder.blur().size(new SizeState(w, h)).radius(new QuadRadiusState(3f))
                .color(new QuadColorState(new Color(140, 140, 140, (int) (255 * a  )))).blurRadius(11f).smoothness(1f).build().render(matrix, x, y);
    }

    private Color alpha(Color c, float a) {
        a = Math.max(0f, Math.min(1f, a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * a));
    }

    private void updateRowAnimations(Map<String, Animation> map, Set<String> active) {
        for (Map.Entry<String, Animation> entry : new ArrayList<>(map.entrySet())) {
            Animation anim = entry.getValue();
            anim.update();
            anim.start(anim.getValue(), active.contains(entry.getKey()) ? 1f : 0f, 250, Easing.CUBIC_OUT);
        }
        for (String key : active) {
            if (!map.containsKey(key)) {
                Animation anim = new Animation();
                anim.setValue(0f);
                map.put(key, anim);
            }
        }
        map.entrySet().removeIf(e -> !active.contains(e.getKey()) && e.getValue().getValue() < 0.01f);
    }

    private float updateSlider(Module m) {
        Animation anim = keybindSliders.get(m.getName());
        if (anim == null) {
            anim = new Animation();
            anim.setValue(m.isEnabled() ? 1f : 0f);
            keybindSliders.put(m.getName(), anim);
        }
        anim.update();
        anim.start(anim.getValue(), m.isEnabled() ? 1f : 0f, 300, Easing.CUBIC_OUT);
        return anim.getValue();
    }

    private String getEffectBaseName(StatusEffectInstance effect) {
        String name = effect.getEffectType().value().getName().getString();
        if (name.equals("Fire Resistance")) name = "Ognestoi";
        return name;
    }

    private String potionKey(StatusEffectInstance effect) {
        return getEffectBaseName(effect) + ":" + effect.getAmplifier();
    }

    private String fullPotionName(StatusEffectInstance effect) {
        String name = getEffectBaseName(effect);
        int amp = effect.getAmplifier() + 1;
        if (amp > 1) name += " " + amp;
        return name;
    }

    private String formatDuration(StatusEffectInstance effect) {
        int ticks = effect.getDuration();
        if (ticks > 32767) return "**:**";
        return String.format("%d:%02d", (ticks / 20) / 60, (ticks / 20) % 60);
    }

    private void drawSprite(MatrixStack matrices, Sprite sprite, float x, float y, float size) {
        float x1 = x, x2 = x + size, y1 = y, y2 = y + size;
        RenderSystem.setShaderTexture(0, sprite.getAtlasId());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f m = matrices.peek().getPositionMatrix();
        buf.vertex(m, x1, y1, 0).texture(sprite.getMinU(), sprite.getMinV()).color(255, 255, 255, 255);
        buf.vertex(m, x1, y2, 0).texture(sprite.getMinU(), sprite.getMaxV()).color(255, 255, 255, 255);
        buf.vertex(m, x2, y2, 0).texture(sprite.getMaxU(), sprite.getMaxV()).color(255, 255, 255, 255);
        buf.vertex(m, x2, y1, 0).texture(sprite.getMaxU(), sprite.getMinV()).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private ItemStack placeholderForSlot(int slot) {
        return switch (slot) {
            case 3 -> new ItemStack(Items.DIAMOND_HELMET);
            case 2 -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            case 1 -> new ItemStack(Items.DIAMOND_LEGGINGS);
            case 0 -> new ItemStack(Items.DIAMOND_BOOTS);
            default -> ItemStack.EMPTY;
        };
    }

    private String durabilityText(ItemStack stack, boolean chat) {
        if (!stack.isEmpty() && stack.isDamageable() && stack.getMaxDamage() > 0) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getDamage();
            int percent = (int) (((maxDamage - currentDamage) / (float) maxDamage) * 100);
            return percent + "%";
        } else if (chat) {
            return "100%";
        }
        return "";
    }

    private record ArmorRow(String key, ItemStack stack, String durability, boolean placeholder) {}
}

