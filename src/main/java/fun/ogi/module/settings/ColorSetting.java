package fun.ogi.module.settings;

import fun.ogi.module.Module;
import java.awt.*;
import java.util.function.Supplier;

public class ColorSetting extends Setting {
    private Color value;
    private boolean rainbow;

    public ColorSetting(String name, Module parent, Color defaultColor) {
        super(name, parent);
        this.value = defaultColor;
    }

    public Color getValue() {
        return value;
    }

    public void setValue(Color value) {
        this.value = value;
    }

    public int getRGB() {
        return value.getRGB();
    }
    public ColorSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public boolean isRainbow() {
        return rainbow;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    @Override
    public String getValueAsString() {
        return String.format("%d,%d,%d,%d", value.getRed(), value.getGreen(), value.getBlue(), value.getAlpha());
    }

    @Override
    public void setValueFromString(String value) {
        try {
            String[] split = value.split(",");
            if (split.length == 4) {
                this.value = new Color(
                        Integer.parseInt(split[0]),
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2]),
                        Integer.parseInt(split[3])
                );
            }
        } catch (Exception ignored) {}
    }
}

