package fun.ogi.util.render.providers;

import fun.ogi.module.theme.ThemeManager;
import java.awt.*;

public final class ColorProvider {

    public static int pack(int red, int green, int blue, int alpha) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | ((blue & 0xFF) << 0);
    }

    public static int[] unpack(int color) {
        return new int[] {color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF};
    }

    public static float[] normalize(Color color) {
        return new float[] {color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f};
    }

    public static float[] normalize(int color) {
        int[] components = unpack(color);
        return new float[] {components[0] / 255.0f, components[1] / 255.0f, components[2] / 255.0f, components[3] / 255.0f};
    }

    public static int getColorClient() {
        return ThemeManager.getInstance().getPrimary();
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int alpha(int color) {
        return (color >> 24) & 0xFF;
    }

    public static int interpolateColor(int color1, int color2, float amount) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        int r = (int) (r1 + (r2 - r1) * amount);
        int g = (int) (g1 + (g2 - g1) * amount);
        int b = (int) (b1 + (b2 - b1) * amount);
        int a = (int) (a1 + (a2 - a1) * amount);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

}