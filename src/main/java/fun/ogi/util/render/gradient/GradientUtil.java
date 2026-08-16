package fun.ogi.util.render.gradient;

import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.msdf.MsdfFont;
import fun.ogi.util.render.providers.ColorProvider;
import org.joml.Matrix4f;

import java.awt.Color;

public final class GradientUtil {

    private GradientUtil() {
    }

    public static void drawDiagonalBackground(Matrix4f matrix, float x, float y, float w, float h,
                                              float radius, Color start, Color end) {
        Builder.rectangle()
                .size(new SizeState(w, h))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(start, end, start, end))
                .build().render(matrix, x, y);
    }


    public static void drawDiagonalBackgroundFallback(Matrix4f matrix, float x, float y, float w, float h,
                                                      float radius, Color start, Color end) {
        int steps = 24;
        float stepW = w / steps;
        for (int i = 0; i < steps; i++) {
            float t = (float) i / (steps - 1);
            Color c = interpolate(start, end, t);
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

    public static void renderAnimatedGradientText(Matrix4f matrix, String text, MsdfFont font, float size,
                                                  float thickness, float x, float y, Color start, Color end) {
        int len = text.length();
        if (len == 0) return;

        float thicknessAdvance = thickness * 0.5f * size;
        int drawnCount = 0;
        double time = System.currentTimeMillis() * 0.005;
        for (int i = 0; i < len; i++) {
            int _char = text.charAt(i);
            if (!font.getGlyphs().containsKey(_char)) continue;

            float wave = (float) ((Math.sin(time - (double) i / len * Math.PI * 2.0) + 1.0) * 0.5);
            Color c = interpolate(start, end, wave);
            float posX = x + font.getWidth(text.substring(0, i), size) + thicknessAdvance * drawnCount;
            Builder.text().text(String.valueOf(text.charAt(i))).font(font).size(size).thickness(thickness)
                    .color(c).build().render(matrix, posX, y);
            drawnCount++;
        }
    }

    public static Color interpolate(Color start, Color end, float t) {
        return new Color(ColorProvider.interpolateColor(start.getRGB(), end.getRGB(), t));
    }
}