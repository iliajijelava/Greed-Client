package fun.ogi.module.settings;

import fun.ogi.module.Module;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ListSetting extends Setting {
    private final List<String> options;
    private final Set<String> selected = new LinkedHashSet<>();

    public ListSetting(String name, Module parent, String... options) {
        super(name, parent);
        this.options = Arrays.asList(options);
    }

    public List<String> getOptions() {
        return options;
    }

    public Set<String> getSelected() {
        return selected;
    }

    public boolean isSelected(String option) {
        return selected.contains(option);
    }
    public ListSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public void toggle(String option) {
        if (option != null && options.contains(option)) {
            if (selected.contains(option)) {
                selected.remove(option);
            } else {
                selected.add(option);
            }
        }
    }

    public void select(String option) {
        if (option != null && options.contains(option)) {
            selected.add(option);
        }
    }

    public void deselect(String option) {
        selected.remove(option);
    }

    public void clear() {
        selected.clear();
    }

    public void selectAll() {
        selected.addAll(options);
    }

    @Override
    public String getValueAsString() {
        return String.join(",", selected);
    }

    @Override
    public void setValueFromString(String value) {
        selected.clear();
        if (value == null || value.isEmpty()) return;
        for (String s : value.split(",")) {
            String trimmed = s.trim();
            if (options.contains(trimmed)) {
                selected.add(trimmed);
            }
        }
    }
}

