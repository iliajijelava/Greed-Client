package fun.ogi.util.render;

import fun.ogi.util.render.builders.Builder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.renderers.impl.BuiltShadow;

import java.awt.Color;

public final class ShadowUtil {

    private static final float SOFTNESS = 2.3f;
    private static final float OFFSET = 0.3f;
    private static final int ALPHA = 85;

    private ShadowUtil() {
    }

    public static BuiltShadow dark(float w, float h, QuadRadiusState radius) {
        return Builder.shadow().size(new SizeState(w, h)).radius(radius)
                .color(new QuadColorState(new Color(0, 0, 0, ALPHA)))
                .softness(SOFTNESS).offset(OFFSET, OFFSET).build();
    }

    public static BuiltShadow window(float w, float h, QuadRadiusState radius) {
        return Builder.shadow().size(new SizeState(w, h)).radius(radius)
                .color(new QuadColorState(new Color(0, 0, 0, 70)))
                .softness(4f).offset(0f, 0f).build();
    }

    public static BuiltShadow gradient(Color start, Color end, float w, float h, QuadRadiusState radius) {
        Color s = new Color(start.getRed(), start.getGreen(), start.getBlue(), ALPHA);
        Color e = new Color(end.getRed(), end.getGreen(), end.getBlue(), ALPHA);
        return Builder.shadow().size(new SizeState(w, h)).radius(radius)
                .color(new QuadColorState(s, e, s, e))
                .softness(SOFTNESS).offset(OFFSET, OFFSET).build();
    }
}

