package fun.ogi.module.impl.list.render;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import fun.ogi.Cheap;
import fun.ogi.module.Module;
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
import fun.ogi.util.render.providers.ColorProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModuleList {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final Draggable draggable = new Draggable(5, 25, 100, 20);

    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());

    private final Map<String, Animation> rowAnimations = new HashMap<>();

    public void render(DrawContext context, String style, String mode, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (mc.player == null || mc.world == null) return;

        List<Module> enabled = Cheap.getInstance().getModuleStorage().getModules().stream()
                .filter(Module::isEnabled)
                .toList();
        if (enabled.isEmpty()) {
            rowAnimations.clear();
            return;
        }

        Set<String> enabledNames = new HashSet<>();
        for (Module m : enabled) enabledNames.add(m.getName());

        for (Map.Entry<String, Animation> entry : new ArrayList<>(rowAnimations.entrySet())) {
            Animation a = entry.getValue();
            a.update();
            a.start(a.getValue(), enabledNames.contains(entry.getKey()) ? 1f : 0f, 250, Easing.QUART_OUT);
        }
        for (Module m : enabled) {
            if (!rowAnimations.containsKey(m.getName())) {
                Animation a = new Animation();
                a.setValue(0f);
                rowAnimations.put(m.getName(), a);
            }
        }
        rowAnimations.entrySet().removeIf(e -> !enabledNames.contains(e.getKey()) && e.getValue().getValue() < 0.01f);

        if (mc.currentScreen instanceof ChatScreen) {
            draggable.onDraw(mouseX, mouseY, screenWidth, screenHeight);
        }

        boolean rightSide = draggable.getX() + draggable.getWidth() / 2f > screenWidth / 2f;

        MsdfFont font = BIKO_FONT.get();
        float textSize = 8f;
        float rowHeight = 11f;
        float pad = 5f;
        float barW = 2f;
        float barGap = 3f;
        float rounding = 0f;

        List<Module> rows = new ArrayList<>(enabled);
        rows.sort((a, b) -> Float.compare(font.getWidth(b.getName(), textSize), font.getWidth(a.getName(), textSize)));

        int count = rows.size();
        float[] widths = new float[count];
        float maxW = 0f;
        for (int i = 0; i < count; i++) {
            widths[i] = font.getWidth(rows.get(i).getName(), textSize) + pad * 2f + barW + barGap;
            maxW = Math.max(maxW, widths[i]);
        }
        boolean isOld = mode.equals("Old");
        float rowGap = isOld ? 2f : 0f;
        float totalH = count * rowHeight + (count - 1) * rowGap;

        draggable.setWidth(maxW);
        draggable.setHeight(totalH);

        float x = draggable.getX();
        float y = draggable.getY();
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        Color gradStart = isOld ? new Color(ThemeManager.getInstance().getPrimary()) : null;
        Color gradEnd = isOld ? new Color(ThemeManager.getInstance().getSecondary()) : null;

        for (int i = 0; i < count; i++) {
            Module m = rows.get(i);
            Animation a = rowAnimations.get(m.getName());
            float anim = a != null ? a.getValue() : 1f;
            if (anim <= 0.01f) continue;

            float rowY = y + i * (rowHeight + rowGap);
            float rowW = isOld ? font.getWidth(m.getName(), textSize) + pad * 2f : widths[i];

            float offset = (1f - anim) * 14f * (rightSide ? 1f : -1f);
            float rowX = x + offset + (rightSide ? maxW - rowW : 0f);

            
            if (isOld) {
                ShadowUtil.gradient(gradStart, gradEnd, rowW, rowHeight, new QuadRadiusState(3f)).render(matrix, rowX, rowY, 0);
                Builder.rectangle().size(new SizeState(rowW, rowHeight)).radius(new QuadRadiusState(3f))
                        .color(new QuadColorState(gradStart, gradEnd, gradStart, gradEnd))
                        .build().render(matrix, rowX, rowY);
            } else {
                renderRowBackground(matrix, style, rowX, rowY, rowW, rowHeight, rounding);
            }

            
            if (!isOld) {
                float barX = rightSide ? rowX + rowW - barW : rowX;
                int rowColor = rowColor(m);
                Builder.rectangle().size(new SizeState(barW, rowHeight)).radius(new QuadRadiusState(barW / 2f))
                        .color(new QuadColorState(withAlpha(rowColor, anim))).build().render(matrix, barX, rowY);
            }

            float textY = rowY + (rowHeight - textSize) / 2f;
            float textX;
            if (rightSide) {
                if (isOld) {
                    textX = rowX + rowW - pad - font.getWidth(m.getName(), textSize);
                } else {
                    textX = rowX + rowW - (barW + barGap + pad) - font.getWidth(m.getName(), textSize);
                }
            } else {
                textX = rowX + (isOld ? pad : barW + barGap + pad);
            }

            if (isOld) {
                
                Builder.text().text(m.getName()).font(font).size(textSize).thickness(0.06f).color(Color.WHITE)
                        .build().render(matrix, textX, textY);
            } else {
                
                renderGradientText(matrix, m.getName(), font, textSize, 0.06f, textX, textY, anim);
            }
        }
    }

    private int rowColor(Module m) {
        return ThemeManager.getInstance().getPrimary();
    }

    private void renderGradientText(Matrix4f matrix, String text, MsdfFont font, float size, float thickness,
                                    float x, float y, float alpha) {
        int len = text.length();
        if (len == 0) return;
        ThemeManager tm = ThemeManager.getInstance();
        int start = tm.getPrimary();
        int end = tm.isRainbow() ? start : tm.getSecondary();
        float thicknessAdvance = (thickness + 0.0f) * 0.5f * size;
        int drawnCount = 0;
        double time = System.currentTimeMillis() * 0.005;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (!font.getGlyphs().containsKey((int) c)) continue;
            float wave = tm.isRainbow() ? 0f : (float) ((Math.sin(time - (double) i / len * Math.PI * 2.0) + 1.0) * 0.5);
            int col = ColorProvider.interpolateColor(start, end, wave);
            float posX = x + font.getWidth(text.substring(0, i), size) + thicknessAdvance * drawnCount;
            Builder.text().text(String.valueOf(c)).font(font).size(size).thickness(thickness)
                    .color(new Color(withAlpha(col, alpha))).build().render(matrix, posX, y);
            drawnCount++;
        }
    }

    private void renderRowBackground(Matrix4f matrix, String style, float x, float y, float w, float h, float rounding) {
        if (style.equals("Liquid Glass")) {
            Builder.liquid().size(new SizeState(w, h)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(new Color(255, 255, 255, 255))).build().render(matrix, x, y);
        } else if (style.equals("Colored Liquid")) {
            Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
            hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 150);
            Builder.coloredLiquid().size(new SizeState(w, h)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(hudBg)).build().render(matrix, x, y);
        } else if (style.equals("Solid")) {
            Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
            hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 255);
            Builder.blur().size(new SizeState(w, h)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(hudBg)).blurRadius(10).smoothness(1f).build().render(matrix, x, y);
        } else {
            ShadowUtil.dark(w, h, new QuadRadiusState(rounding)).render(matrix, x, y, 0);
            Builder.rectangle().size(new SizeState(w, h)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(new Color(20, 20, 20, 255))).build().render(matrix, x, y);
        }
    }

    private int withAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
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
}