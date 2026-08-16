package fun.ogi.module.settings;

import fun.ogi.module.Module;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeSetting extends Setting {
    private String value;
    private final List<String> modes;

    public ModeSetting(String name, Module parent, String defaultValue, String... modes) {
        super(name, parent);
        this.value = defaultValue;
        this.modes = Arrays.asList(modes);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (modes.contains(value)) {
            this.value = value;
        }
    }
    public ModeSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public boolean is(String mode) {
        return value.equals(mode);
    }
    public List<String> getModes() {
        return modes;
    }

    public int getIndex() {
        return modes.indexOf(value);
    }

    public void setIndex(int index) {
        if (index >= 0 && index < modes.size()) {
            this.value = modes.get(index);
        }
    }

    public void cycle() {
        int nextIndex = (getIndex() + 1) % modes.size();
        setValue(modes.get(nextIndex));
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        if (modes.contains(value)) {
            this.value = value;
        }
    }
}