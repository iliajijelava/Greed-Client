package fun.ogi.util.render.builders;


import fun.ogi.util.render.builders.impl.*;

public final class Builder {

    private static final RectangleBuilder RECTANGLE_BUILDER = new RectangleBuilder();
    private static final BorderBuilder BORDER_BUILDER = new BorderBuilder();
    private static final TextureBuilder TEXTURE_BUILDER = new TextureBuilder();
    private static final TextBuilder TEXT_BUILDER = new TextBuilder();
    private static final BlurBuilder BLUR_BUILDER = new BlurBuilder();
    private static final LiquidBuilder LIQUID_BUILDER = new LiquidBuilder();
    private static final ShadowBuilder SHADOW_BUILDER = new ShadowBuilder();
    private static final ColoredLiquidBuilder COLORED_LIQUID_BUILDER = new ColoredLiquidBuilder();

    public static RectangleBuilder rectangle() {
        return RECTANGLE_BUILDER;
    }

    public static BorderBuilder border() {
        return BORDER_BUILDER;
    }

    public static TextureBuilder texture() {
        return TEXTURE_BUILDER;
    }

    public static TextBuilder text() {
        return TEXT_BUILDER;
    }

    public static BlurBuilder blur() {
        return BLUR_BUILDER;
    }

    public static LiquidBuilder liquid() {
        return LIQUID_BUILDER;
    }

    public static ShadowBuilder shadow() {
        return SHADOW_BUILDER;
    }

    public static ColoredLiquidBuilder coloredLiquid() {
        return COLORED_LIQUID_BUILDER;
    }

}