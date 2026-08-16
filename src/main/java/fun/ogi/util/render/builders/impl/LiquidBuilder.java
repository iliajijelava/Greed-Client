package fun.ogi.util.render.builders.impl;

import fun.ogi.util.render.builders.AbstractBuilder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.renderers.impl.BuiltLiquid;

public final class LiquidBuilder extends AbstractBuilder<BuiltLiquid> {

    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;

    public LiquidBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public LiquidBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public LiquidBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    @Override
    protected BuiltLiquid _build() {
        return new BuiltLiquid(
                this.size,
                this.radius,
                this.color
        );
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.TRANSPARENT;
    }

}

