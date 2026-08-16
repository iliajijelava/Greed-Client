package fun.ogi.module.settings;

import fun.ogi.module.Module;

import java.util.function.Supplier;

public class NumberSetting extends Setting {
    private double value;
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, Module parent, double defaultValue, double min, double max, double step) {
        super(name, parent);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getValue() {
        return value;
    }
    public NumberSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public int getIntValue() {
        return (int) value;
    }

    public float getFloatValue() {
        return (float) value;
    }

    public void setValue(double value) {
        value = Math.round(value / step) * step;
        this.value = Math.max(min, Math.min(max, value));
    }

    public void increment() {
        setValue(value + step);
    }

    public void decrement() {
        setValue(value - step);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    @Override
    public String getValueAsString() {
        if (step >= 1) return String.valueOf((int) value);
        if (step >= 0.1) return String.format("%.1f", value);
        return String.format("%.2f", value);
    }

    @Override
    public void setValueFromString(String value) {
        try {
            setValue(Double.parseDouble(value));
        } catch (NumberFormatException ignored) {}
    }
}