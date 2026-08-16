package fun.ogi.module.settings;

import fun.ogi.module.Module;

import java.util.function.Supplier;

public class StringSetting extends Setting {
    private String value;

    public StringSetting(String name, Module parent, String defaultValue) {
        super(name, parent);
        this.value = defaultValue;
    }

    public String getText() {
        return value;
    }

    public void setText(String value) {
        this.value = value;
    }
    public StringSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        this.value = value;
    }
}

