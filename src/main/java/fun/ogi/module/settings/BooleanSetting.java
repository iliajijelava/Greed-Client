package fun.ogi.module.settings;

import fun.ogi.module.Module;

import java.util.function.Supplier;

public class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String name, Module parent, boolean defaultValue) {
        super(name, parent);
        this.value = defaultValue;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }
    public BooleanSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    @Override
    public String getValueAsString() {
        return Boolean.toString(value);
    }

    @Override
    public void setValueFromString(String value) {
        this.value = Boolean.parseBoolean(value);
    }

    public void toggle() {
        this.value = !this.value;
    }
}