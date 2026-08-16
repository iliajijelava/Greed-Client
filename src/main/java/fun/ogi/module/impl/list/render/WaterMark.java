package fun.ogi.module.impl.list.render;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.ShadowUtil;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.providers.ColorProvider;
import fun.ogi.util.render.renderers.impl.BuiltShadow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import org.joml.Matrix4f;

import java.awt.Color;

public class WaterMark {

    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() ->
            MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() ->
            MsdfFont.builder().atlas("nur").data("nur").build());
    private static final Supplier<MsdfFont> EXTRABOLD_FONT = Suppliers.memoize(() ->
            MsdfFont.builder().atlas("regular_semibold").data("regular_semibold").build());
    private static final Supplier<MsdfFont> MACAN_ICONS2_FONT = Suppliers.memoize(() ->
            MsdfFont.builder().atlas("macan_icons2").data("macan_icons2").build());

    private final float posX = 10;
    private final float posY = 10;
    private float smoothFps = 0;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public void render(DrawContext context, String style, String mode) {
        if (mc.player == null || mc.world == null) return;

        if (mode.equals("Macan")) {
            renderMacan(context);
            return;
        }
        if (mode.equals("Old")) {
            renderOld(context);
            return;
        }
        if (mode.equals("YouGame")) {
            renderYouGame(context);
            return;
        }
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        smoothFps += (mc.getCurrentFps() - smoothFps) * 0.15f;

        String buildText = "Greed";
        String nameText = mc.getSession().getUsername();
        String fpsText = Math.round(smoothFps) + " Fps";
        String pingText = getPing() + " Ping";

        Color accentColor = new Color(ThemeManager.getInstance().getPrimary());
        Color textColor = Color.WHITE;
        Color hudBg = new Color(ThemeManager.getInstance().getPalette().getHudBackground());
        hudBg = new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 255);
        Color sepColor = new Color(166, 166, 166, 255);

        float ts = 9f;
        float iconSz = 8f;
        float pad = 7f;
        float gap = 5f;
        float textThick = 0.06f;
        float iconThick = 0.08f;
        float sepW = 0.5f;
        float sepPad = 3f;
        float blockH = 16f;
        float rounding = 4f;

        MsdfFont biko = BIKO_FONT.get();
        MsdfFont icon = ICON_FONT.get();

        float buildW = biko.getWidth(buildText, ts);
        float nameW = biko.getWidth(nameText, ts);
        float fpsW = biko.getWidth(fpsText, ts);
        float pingW = biko.getWidth(pingText, ts);

        float iconNickW = icon.getWidth("W", iconSz);
        float iconFpsW = icon.getWidth("V", iconSz);
        float iconPingW = icon.getWidth("$", iconSz);

        float totalW = pad
                + gap + buildW + gap + sepW + sepPad
                + iconNickW + gap + nameW + gap + sepW + sepPad
                + iconFpsW + gap + fpsW + gap + sepW + sepPad
                + iconPingW + gap + pingW
                + pad;

        if (style.equals("Liquid Glass")) {
            Builder.liquid().size(new SizeState(totalW, blockH)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(new Color(255, 255, 255, 255))).build().render(matrix, posX, posY);
        } else if (style.equals("Colored Liquid")) {
            Builder.coloredLiquid().size(new SizeState(totalW, blockH)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(new Color(hudBg.getRed(), hudBg.getGreen(), hudBg.getBlue(), 150))).build().render(matrix, posX, posY);
        } else if (style.equals("Default")) {
            BuiltShadow shadow = Builder.shadow().size(new SizeState(totalW , blockH )).radius(new QuadRadiusState(4))
                    .color(new QuadColorState(new Color(0, 0, 0, 35))).softness(2.3f).offset(0.1f, 0.1f).build();
            shadow.render(matrix, posX,posY, 0);
            Builder.rectangle().size(new SizeState(totalW, blockH)).radius(new QuadRadiusState(4)).color(new QuadColorState(new Color(20, 20, 20, 255))).build().render(matrix, posX, posY);
        } else {
            Builder.blur().size(new SizeState(totalW, blockH)).radius(new QuadRadiusState(rounding))
                    .color(new QuadColorState(hudBg)).blurRadius(10).smoothness(1f).build().render(matrix, posX, posY);
        }

        float cx = posX + pad;
        float centerY = posY + blockH / 2f;
        float iconY = centerY - iconSz / 2f;
        float textY = centerY - ts / 2f;

        Color gradientStart = new Color(ThemeManager.getInstance().getPrimary());
        Color gradientEnd = new Color(ThemeManager.getInstance().getSecondary());



        cx += gap;
        renderGradientText(matrix, buildText, biko, ts, textThick, cx - 1.5f, textY, gradientStart, gradientEnd);
        cx += buildW + gap;
        Builder.rectangle().size(new SizeState(sepW, blockH - 5f)).color(new QuadColorState(sepColor))
                .build().render(matrix, cx, posY + 2.5f, 0);
        cx += sepW + sepPad;

        Builder.text().text("W").font(icon).size(iconSz).thickness(iconThick).color(accentColor)
                .build().render(matrix, cx, iconY);
        cx += iconNickW + gap;
        Builder.text().text(nameText).font(biko).size(ts).thickness(textThick).color(textColor)
                .build().render(matrix, cx, textY);
        cx += nameW + gap;
        Builder.rectangle().size(new SizeState(sepW, blockH - 5f)).color(new QuadColorState(sepColor))
                .build().render(matrix, cx, posY + 2.5f, 0);
        cx += sepW + sepPad;

        Builder.text().text("V").font(icon).size(iconSz).thickness(iconThick).color(accentColor)
                .build().render(matrix, cx, iconY);
        cx += iconFpsW + gap;
        Builder.text().text(fpsText).font(biko).size(ts).thickness(textThick).color(textColor)
                .build().render(matrix, cx, textY);
        cx += fpsW + gap;
        Builder.rectangle().size(new SizeState(sepW, blockH - 5f)).color(new QuadColorState(sepColor))
                .build().render(matrix, cx, posY + 2.5f, 0);
        cx += sepW + sepPad;

        Builder.text().text("$").font(icon).size(iconSz).thickness(iconThick).color(accentColor)
                .build().render(matrix, cx, iconY);
        cx += iconPingW + gap;
        Builder.text().text(pingText).font(biko).size(ts).thickness(textThick).color(textColor)
                .build().render(matrix, cx, textY);
    }

    private void renderGradientText(Matrix4f matrix, String text, MsdfFont font, float size, float thickness,
                                    float x, float y, Color start, Color end) {
        int len = text.length();
        if (len == 0) return;

        float thicknessAdvance = (thickness + 0.0f) * 0.5f * size;
        int drawnCount = 0;
        double time = System.currentTimeMillis() * 0.005;
        for (int i = 0; i < len; i++) {
            int _char = (int) text.charAt(i);
            if (!font.getGlyphs().containsKey(_char)) continue;

            float wave = (float) ((Math.sin(time - (double) i / len * Math.PI * 2.0) + 1.0) * 0.5);
            Color c = new Color(ColorProvider.interpolateColor(start.getRGB(), end.getRGB(), wave));
            float posX = x + font.getWidth(text.substring(0, i), size) + thicknessAdvance * drawnCount;
            Builder.text().text(String.valueOf(text.charAt(i))).font(font).size(size).thickness(thickness)
                    .color(c).build().render(matrix, posX, y);
            drawnCount++;
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

    





    private void drawGradientBackgroundFallback(Matrix4f matrix, float x, float y, float w, float h,
                                                float radius, Color start, Color end) {
        int steps = 24;
        float stepW = w / steps;
        for (int i = 0; i < steps; i++) {
            float t = (float) i / (steps - 1);
            Color c = new Color(ColorProvider.interpolateColor(start.getRGB(), end.getRGB(), t));
            
            QuadRadiusState r = (i == 0)
                    ? new QuadRadiusState(radius, 0, radius, 0)
                    : (i == steps - 1)
                    ? new QuadRadiusState(0, radius, 0, radius)
                    : new QuadRadiusState(0);
            Builder.rectangle()
                    .size(new SizeState(stepW + 1f, h)) 
                    .radius(r)
                    .color(new QuadColorState(c))
                    .build().render(matrix, x + i * stepW, y);
        }
    }

    private void renderMacan(DrawContext context) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        smoothFps += (mc.getCurrentFps() - smoothFps) * 0.15f;

        String brandText = "GREED ";
        String username = mc.getSession().getUsername();
        String fpsText = String.valueOf(Math.max(0, Math.round(smoothFps)));
        String pingText = String.valueOf(Math.max(0, getPing()));

        float backHeight = 20f;
        float horPad = 5f;
        float sectionGap = 5f;
        float centerY = posY + backHeight / 2f;

        MsdfFont brandFont = EXTRABOLD_FONT.get();
        MsdfFont valueFont = BIKO_FONT.get();
        MsdfFont labelFont = BIKO_FONT.get();
        MsdfFont iconFont = MACAN_ICONS2_FONT.get();

        float brandWidth = brandFont.getWidth(brandText, 10f);
        float back1X = posX;
        float back2X = back1X + horPad + brandWidth + horPad;
        float back1Width = back2X - back1X;

        float fpsGlyphW = iconFont.getWidth("L", 8f);
        float pingGlyphW = iconFont.getWidth("a", 8f);
        float usernameW = valueFont.getWidth(username, 8f);
        float fpsW = valueFont.getWidth(fpsText, 8f);
        float fpsLabelW = labelFont.getWidth("fps", 7f);
        float pingW = valueFont.getWidth(pingText, 8f);
        float pingLabelW = labelFont.getWidth("ms", 7f);

        float back2Width = horPad + sectionGap + usernameW + sectionGap + 2 + sectionGap + fpsGlyphW + 2 + fpsW + 2 + fpsLabelW + sectionGap + 2 + sectionGap + pingGlyphW + 2 + pingW + 2 + pingLabelW + horPad;

        Color accentColor = new Color(ThemeManager.getInstance().getPrimary());
        Color leftBg = new Color(12, 12, 12, 204);
        Color rightBg = new Color(22, 22, 22, 153);
        Color dividerColor = new Color(200, 200, 200, 128);
        Color labelColor = new Color(200, 200, 200, 179);
        BuiltShadow shadow = Builder.shadow().size(new SizeState(back1Width , backHeight )).radius(new QuadRadiusState(5,5,0,0))
                .color(new QuadColorState(new Color(0, 0, 0, 35))).softness(2.3f).offset(0.1f, 0.1f).build();
        shadow.render(matrix, posX,posY, 0);
        ShadowUtil.dark(back2Width, backHeight, new QuadRadiusState(0, 0, 5, 5)).render(matrix, back2X, posY, 0);
        Builder.blur().size(new SizeState(back1Width, backHeight)).radius(new QuadRadiusState(5, 5, 0, 0))
                .color(new QuadColorState(leftBg)).blurRadius(10).smoothness(1f).build().render(matrix, back1X, posY);
        Builder.blur().size(new SizeState(back2Width, backHeight)).radius(new QuadRadiusState(0, 0, 5, 5))
                .color(new QuadColorState(rightBg)).blurRadius(10).smoothness(1f).build().render(matrix, back2X, posY);

        float brandX = back1X + horPad;
        Builder.text().text(brandText).font(brandFont).size(10f).thickness(0.1f).color(accentColor)
                .build().render(matrix, brandX, centerY - 7f);

        float contentX = back2X + horPad;

        Builder.text().text(username).font(valueFont).size(8f).thickness(0.06f).color(Color.WHITE)
                .build().render(matrix, contentX, centerY - 4f);
        contentX += usernameW + sectionGap + 1;

        contentX = drawMacanSep(matrix, contentX, centerY, dividerColor);

        Builder.text().text("L").font(iconFont).size(8f).thickness(0.08f).color(accentColor)
                .build().render(matrix, contentX, centerY - 4f);
        contentX += fpsGlyphW + 2;

        Builder.text().text(fpsText).font(valueFont).size(8f).thickness(0.06f).color(Color.WHITE)
                .build().render(matrix, contentX, centerY - 4f);
        contentX += fpsW + 2;

        Builder.text().text("fps").font(labelFont).size(7f).thickness(0.06f).color(labelColor)
                .build().render(matrix, contentX, centerY - 3.5f);
        contentX += fpsLabelW + sectionGap;

        contentX = drawMacanSep(matrix, contentX, centerY, dividerColor);

        Builder.text().text("a").font(iconFont).size(8f).thickness(0.08f).color(accentColor)
                .build().render(matrix, contentX, centerY - 4f);
        contentX += pingGlyphW + 2;

        Builder.text().text(pingText).font(valueFont).size(8f).thickness(0.06f).color(Color.WHITE)
                .build().render(matrix, contentX, centerY - 4f);
        contentX += pingW + 2;

        Builder.text().text("ms").font(labelFont).size(7f).thickness(0.06f).color(labelColor)
                .build().render(matrix, contentX, centerY - 3.5f);
    }

    private void renderOld(DrawContext context) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        smoothFps += (mc.getCurrentFps() - smoothFps) * 0.15f;

        String buildText = "Greed";
        String nameText = mc.getSession().getUsername();
        String fpsText = Math.round(smoothFps) + " Fps";
        String pingText = getPing() + " Ping";

        Color textColor = Color.WHITE;
        Color iconColor = Color.WHITE;
        Color sepColor = new Color(255, 255, 255, 140);

        float ts = 9f;
        float iconSz = 8f;
        float pad = 7f;
        float gap = 5f;
        float textThick = 0.06f;
        float iconThick = 0.08f;
        float sepW = 0.5f;
        float sepPad = 3f;
        float blockH = 22f;
        float rounding = 4f;

        MsdfFont biko = BIKO_FONT.get();
        MsdfFont icon = ICON_FONT.get();

        float buildW = biko.getWidth(buildText, ts);
        float nameW = biko.getWidth(nameText, ts);
        float fpsW = biko.getWidth(fpsText, ts);
        float pingW = biko.getWidth(pingText, ts);

        float iconNickW = icon.getWidth("W", iconSz);
        float iconFpsW = icon.getWidth("V", iconSz);
        float iconPingW = icon.getWidth("$", iconSz);

        float totalW = pad
                + gap + buildW + gap + sepW + sepPad
                + iconNickW + gap + nameW + gap + sepW + sepPad
                + iconFpsW + gap + fpsW + gap + sepW + sepPad
                + iconPingW + gap + pingW
                + pad;

        Color gradientStart = new Color(ThemeManager.getInstance().getPrimary());
        Color gradientEnd = new Color(ThemeManager.getInstance().getSecondary());
        BuiltShadow shadow = Builder.shadow().size(new SizeState(totalW , blockH )).radius(new QuadRadiusState(4))
                .color(new QuadColorState(gradientStart,gradientEnd,gradientStart,gradientEnd)).softness(2.3f).offset(0.1f, 0.1f).build();
        shadow.render(matrix, posX,posY, 0);
        drawGradientBackground(matrix, posX, posY, totalW, blockH, rounding, gradientStart, gradientEnd);

        float cx = posX + pad;
        float centerY = posY + blockH / 2f;
        float iconY = centerY - iconSz / 2f;
        float textY = centerY - ts / 2f;

        cx += gap;
        Builder.text().text(buildText).font(biko).size(ts).thickness(textThick).color(textColor)
                .build().render(matrix, cx, textY);
        cx += buildW + gap;
        Builder.rectangle().size(new SizeState(sepW, blockH - 5f)).color(new QuadColorState(sepColor))
                .build().render(matrix, cx, posY + 2.5f, 0);
        cx += sepW + sepPad;

        Builder.text().text("W").font(icon).size(iconSz).thickness(iconThick).color(iconColor)
                .build().render(matrix, cx, iconY);
        cx += iconNickW + gap;
        Builder.text().text(nameText).font(biko).size(ts).thickness(textThick).color(textColor)
                .build().render(matrix, cx, textY);
        cx += nameW + gap;
        Builder.rectangle().size(new SizeState(sepW, blockH - 5f)).color(new QuadColorState(sepColor))
                .build().render(matrix, cx, posY + 2.5f, 0);
        cx += sepW + sepPad;

        Builder.text().text("V").font(icon).size(iconSz).thickness(iconThick).color(iconColor)
                .build().render(matrix, cx, iconY);
        cx += iconFpsW + gap;
        Builder.text().text(fpsText).font(biko).size(ts).thickness(textThick).color(textColor)
                .build().render(matrix, cx, textY);
        cx += fpsW + gap;
        Builder.rectangle().size(new SizeState(sepW, blockH - 5f)).color(new QuadColorState(sepColor))
                .build().render(matrix, cx, posY + 2.5f, 0);
        cx += sepW + sepPad;

        Builder.text().text("$").font(icon).size(iconSz).thickness(iconThick).color(iconColor)
                .build().render(matrix, cx, iconY);
        cx += iconPingW + gap;
        Builder.text().text(pingText).font(biko).size(ts).thickness(textThick).color(textColor)
                .build().render(matrix, cx, textY);
    }

    private void renderYouGame(DrawContext context) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        smoothFps += (mc.getCurrentFps() - smoothFps) * 0.15f;

        String buildText = "Greed";
        String nameText = mc.getSession().getUsername();
        String fpsText = Math.round(smoothFps) + " Fps";
        String pingText = getPing() + " Ping";

        MsdfFont biko = BIKO_FONT.get();
        MsdfFont icon = ICON_FONT.get();

        int accent = ThemeManager.getInstance().getPrimary();

        float sz = 7f;
        float iconSz = 8f;
        float padX = 8f;
        float gap = 5f;
        float sepW = 0.5f;
        float sepPad = 3f;

        float buildW = biko.getWidth(buildText, sz);
        float nameW = biko.getWidth(nameText, sz);
        float fpsW = biko.getWidth(fpsText, sz);
        float pingW = biko.getWidth(pingText, sz);

        float iconNickW = icon.getWidth("W", iconSz);
        float iconFpsW = icon.getWidth("V", iconSz);
        float iconPingW = icon.getWidth("$", iconSz);

        float totalW = padX
                + buildW + gap + sepW + sepPad
                + iconNickW + gap + nameW + gap + sepW + sepPad
                + iconFpsW + gap + fpsW + gap + sepW + sepPad
                + iconPingW + gap + pingW
                + padX;

        float h = 13f;
        youGamePanel(matrix, posX, posY, totalW, h, 1f);

        float cx = posX + padX;
        float centerY = posY + h / 2f;
        float iconY = centerY - iconSz / 2f;
        float textY = centerY - sz / 2f;

        Builder.text().text(buildText).font(biko).size(sz).thickness(0.06f)
                .color(new Color(accent)).build().render(matrix, cx, textY);
        cx += buildW + gap;
        Builder.rectangle().size(new SizeState(sepW, h - 4f))
                .color(new QuadColorState(youGameAlpha(new Color(140, 140, 140), 1f)))
                .build().render(matrix, cx, posY + 2f, 0);
        cx += sepW + sepPad;

        Builder.text().text("W").font(icon).size(iconSz).thickness(0.08f).color(new Color(accent))
                .build().render(matrix, cx, iconY);
        cx += iconNickW + gap;
        Builder.text().text(nameText).font(biko).size(sz).thickness(0.06f).color(Color.WHITE)
                .build().render(matrix, cx, textY);
        cx += nameW + gap;
        Builder.rectangle().size(new SizeState(sepW, h - 4f))
                .color(new QuadColorState(youGameAlpha(new Color(140, 140, 140), 1f)))
                .build().render(matrix, cx, posY + 2f, 0);
        cx += sepW + sepPad;

        Builder.text().text("V").font(icon).size(iconSz).thickness(0.08f).color(new Color(accent))
                .build().render(matrix, cx, iconY);
        cx += iconFpsW + gap;
        Builder.text().text(fpsText).font(biko).size(sz).thickness(0.06f).color(Color.WHITE)
                .build().render(matrix, cx, textY);
        cx += fpsW + gap;
        Builder.rectangle().size(new SizeState(sepW, h - 4f))
                .color(new QuadColorState(youGameAlpha(new Color(140, 140, 140), 1f)))
                .build().render(matrix, cx, posY + 2f, 0);
        cx += sepW + sepPad;

        Builder.text().text("$").font(icon).size(iconSz).thickness(0.08f).color(new Color(accent))
                .build().render(matrix, cx, iconY);
        cx += iconPingW + gap;
        Builder.text().text(pingText).font(biko).size(sz).thickness(0.06f).color(Color.WHITE)
                .build().render(matrix, cx, textY);
    }

    private void youGamePanel(Matrix4f matrix, float x, float y, float w, float h, float alpha) {
        float a = Math.max(0f, Math.min(1f, alpha));
        Builder.rectangle().size(new SizeState(w, h)).radius(new QuadRadiusState(3))
                .color(new QuadColorState(new Color(20, 20, 20, (int) (255 * a)))).build().render(matrix, x, y);
        Builder.blur().size(new SizeState(w, h)).radius(new QuadRadiusState(3))
                .color(new QuadColorState(new Color(140, 140, 140, (int) (255 * a)))).blurRadius(11).smoothness(1f).build().render(matrix, x, y);
    }

    private Color youGameAlpha(Color c, float a) {
        a = Math.max(0f, Math.min(1f, a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * a));
    }

    private float drawMacanSep(Matrix4f matrix, float x, float centerY, Color color) {
        Builder.rectangle().size(new SizeState(1, 10)).radius(new QuadRadiusState(1))
                .color(new QuadColorState(color)).build().render(matrix, x, centerY - 5f);
        return x + 2 + 5;
    }

    private int getPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return 0;
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}