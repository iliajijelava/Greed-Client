package fun.ogi.util.render.builders.impl;

import fun.ogi.util.render.builders.AbstractBuilder;
import fun.ogi.util.render.builders.states.QuadColorState;
import fun.ogi.util.render.builders.states.QuadRadiusState;
import fun.ogi.util.render.builders.states.SizeState;
import fun.ogi.util.render.renderers.impl.BuiltShadow;

public final class ShadowBuilder extends AbstractBuilder<BuiltShadow> {

    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float softness;
    private float offsetX, offsetY;

    public ShadowBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public ShadowBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public ShadowBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public ShadowBuilder softness(float softness) {
        this.softness = softness;
        return this;
    }

    public ShadowBuilder offset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        return this;
    }

    @Override
    protected BuiltShadow _build() {
        return new BuiltShadow(
                this.size,
                this.radius,
                this.color,
                this.softness,
                this.offsetX, this.offsetY
        );
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.TRANSPARENT;
        this.softness = 4.0f;
        this.offsetX = 0.0f;
        this.offsetY = 0.0f;
    }

}

