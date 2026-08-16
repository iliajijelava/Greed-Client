package fun.ogi.module.theme;


import fun.ogi.util.animation.Easing;
import net.minecraft.client.MinecraftClient;

import java.awt.Color;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class ThemeManager {
    private static ThemeManager instance;
    private final List<Theme> themes;
    private final ThemeSerializer serializer;
    private Theme currentTheme;
    private Theme previousTheme;
    private ThemeColorPalette displayPalette;
    private boolean transitioning;
    private long transitionStart;
    private long transitionDuration = 400;
    private Easing transitionEasing = Easing.CUBIC_OUT;
    private float rainbowTime;
    private boolean rainbowCycleActive;
    private final List<Consumer<Theme>> listeners = new ArrayList<>();

    private ThemeManager() {
        this.themes = new ArrayList<>();
        this.serializer = new ThemeSerializer();
        this.displayPalette = new ThemeColorPalette();
        this.transitioning = false;
        this.rainbowTime = 0f;
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void initialize() {
        themes.clear();
        themes.addAll(ThemePresets.getAllPresets());
        themes.addAll(serializer.loadAllCustomThemes());
        String savedThemeName = loadCurrentThemeName();
        Theme defaultTheme = getThemeByName(savedThemeName);
        if (defaultTheme == null) defaultTheme = themes.get(0);
        setThemeInstant(defaultTheme);
    }

    public void tick() {
        rainbowTime += 0.05f;
        if (transitioning) {
            long elapsed = System.currentTimeMillis() - transitionStart;
            if (elapsed >= transitionDuration) {
                transitioning = false;
                displayPalette = ThemeColorPalette.copyOf(currentTheme.getPalette());
                applyMultipliers();
            } else {
                float t = (float) (elapsed / (double) transitionDuration);
                t = (float) transitionEasing.ease((double) t);
                displayPalette = ThemeColorPalette.interpolate(previousTheme.getPalette(), currentTheme.getPalette(), t);
                applyMultipliers();

            }
        }
        if (displayPalette.isRainbow() || displayPalette.isAnimated()) {
            applyMultipliers();

        }
    }

    private void applyMultipliers() {
        displayPalette.applyMultipliers();
    }

    public void setTheme(Theme theme) {
        if (currentTheme != null) {
            previousTheme = currentTheme.copy();
        } else {
            previousTheme = theme.copy();
        }
        currentTheme = theme;
        transitioning = true;
        transitionStart = System.currentTimeMillis();
        saveCurrentThemeName(theme.getName());
        notifyListeners(theme);
    }

    public void setThemeInstant(Theme theme) {
        currentTheme = theme;
        previousTheme = theme.copy();
        transitioning = false;
        displayPalette = ThemeColorPalette.copyOf(theme.getPalette());
        applyMultipliers();
        saveCurrentThemeName(theme.getName());
        notifyListeners(theme);
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public ThemeColorPalette getPalette() {
        if (displayPalette == null) {
            return currentTheme != null ? currentTheme.getPalette() : new ThemeColorPalette();
        }
        return displayPalette;
    }

    public int getPrimary() {
        if (displayPalette.isRainbow()) {
            float hue = (rainbowTime * displayPalette.getSpeed()) % 1.0f;
            return Color.HSBtoRGB(hue, displayPalette.getSaturation(), displayPalette.getBrightness());
        }
        return getPalette().getPrimary();
    }

    public int getSecondary() {
        if (displayPalette.isRainbow()) {
            float hue = ((rainbowTime * displayPalette.getSpeed()) + 0.33f) % 1.0f;
            return Color.HSBtoRGB(hue, displayPalette.getSaturation(), displayPalette.getBrightness());
        }
        return getPalette().getSecondary();
    }

    public int getTertiary() {
        if (displayPalette.isRainbow()) {
            float hue = ((rainbowTime * displayPalette.getSpeed()) + 0.66f) % 1.0f;
            return Color.HSBtoRGB(hue, displayPalette.getSaturation(), displayPalette.getBrightness());
        }
        return getPalette().getTertiary();
    }

    public int getAnimatedPrimary() {
        if (displayPalette.isAnimated() || displayPalette.isRainbow()) {
            float hue = ((rainbowTime * displayPalette.getSpeed()) % 1.0f);
            return Color.HSBtoRGB(hue, 0.8f, 1.0f);
        }
        return getPrimary();
    }

    public boolean isRainbow() {
        return displayPalette != null && displayPalette.isRainbow();
    }

    public boolean isAnimated() {
        return displayPalette != null && displayPalette.isAnimated();
    }

    public float getSpeed() {
        return displayPalette != null ? displayPalette.getSpeed() : 1.0f;
    }

    public List<Theme> getThemes() {
        return themes;
    }

    public Theme getThemeByName(String name) {
        for (Theme t : themes) {
            if (t.getName().equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    public void addTheme(Theme theme) {
        themes.add(theme);
        if (theme.isCustom()) {
            serializer.saveTheme(theme);

        }
    }

    public void removeTheme(Theme theme) {
        themes.remove(theme);
        if (theme.isCustom()) {
            serializer.deleteTheme(theme.getName());

        }
    }

    public void saveCustomTheme(Theme theme) {
        serializer.saveTheme(theme);
    }

    public void addListener(Consumer<Theme> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Theme> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Theme theme) {
        for (Consumer<Theme> l : listeners) {
            l.accept(theme);

        }
    }

    public void setTransitionDuration(long millis) {
        this.transitionDuration = millis;
    }

    public void setTransitionEasing(Easing easing) {
        this.transitionEasing = easing;
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    public float getTransitionProgress() {
        if (!transitioning) return 1.0f;
        long elapsed = System.currentTimeMillis() - transitionStart;
        return Math.min(1.0f, elapsed / (float) transitionDuration);
    }

    public String exportThemeJson(String themeName) {
        Theme t = getThemeByName(themeName);
        if (t == null) return null;
        return serializer.exportThemeAsJson(t);
    }

    public Theme importThemeFromJson(String json) {
        Theme t = serializer.importThemeFromJson(json);
        if (t != null) {
            t.setCustom(true);
            addTheme(t);
        }
        return t;
    }

    private void saveCurrentThemeName(String name) {
        try {
            File configDir = new File(MinecraftClient.getInstance().runDirectory, "ExortWare");
            if (!configDir.exists()) configDir.mkdirs();
            File f = new File(configDir, "current_theme.txt");
            java.nio.file.Files.writeString(f.toPath(), name);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public String loadCurrentThemeName() {
        try {
            File f = new File(MinecraftClient.getInstance().runDirectory, "ExortWare/current_theme.txt");
            if (f.exists()) {
                return java.nio.file.Files.readString(f.toPath()).trim();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Ocean";

    }
}

