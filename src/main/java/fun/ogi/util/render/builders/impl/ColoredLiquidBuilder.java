package fun.ogi.util.render.builders.impl;

import fun.ogi.util.render.builders.AbstractBuilder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.renderers.impl.BuiltColoredLiquid;

public final class ColoredLiquidBuilder extends AbstractBuilder<BuiltColoredLiquid> {

    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float blurAmount;

    public ColoredLiquidBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public ColoredLiquidBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public ColoredLiquidBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public ColoredLiquidBuilder blurAmount(float blurAmount) {
        this.blurAmount = blurAmount;
        return this;
    }

    @Override
    protected BuiltColoredLiquid _build() {
        return new BuiltColoredLiquid(
                this.size,
                this.radius,
                this.color,
                this.blurAmount
        );
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.TRANSPARENT;
        this.blurAmount = 1.5f;
    }

}

