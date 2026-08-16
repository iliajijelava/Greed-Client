package fun.ogi.module.settings;

import fun.ogi.module.Module;

import java.util.function.Supplier;

public abstract class Setting {
    private final String name;
    private final Module parent;
    public Supplier<Boolean> visible = () -> true;
    public Setting(String name, Module parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public Module getParent() {
        return parent;
    }
    public Boolean visible() {
        return visible.get();
    }
    public Setting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public abstract String getValueAsString();
    public abstract void setValueFromString(String value);
}