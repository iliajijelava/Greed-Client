package fun.ogi.module.theme;

import fun.ogi.util.render.providers.ColorProvider;
import java.awt.Color;

public class ThemeColorPalette {
    private int primary;
    private int secondary;
    private int tertiary;
    private int background;
    private int surface;
    private int textPrimary;
    private int textSecondary;
    private int textAccent;
    private int hudBackground;
    private int hudText;
    private int hudAccent;
    private int targetHudBackground;
    private int targetHudHealth;
    private int targetHudAccent;
    private int targetEspFill;
    private int targetEspLine;
    private int circleEsp;
    private int spirits;
    private int wingsPrimary;
    private int wingsSecondary;
    private int wingsGlow;
    private int glow;
    private int blurColor;
    private int gradientStart;
    private int gradientEnd;
    private int progressBar;
    private int progressBarBackground;
    private int toggleOn;
    private int toggleOff;
    private int notificationSuccess;
    private int notificationError;
    private int notificationInfo;
    private int nametagBackground;
    private int nametagHealth;
    private int particlePrimary;
    private int categoryCombat;
    private int categoryMovement;
    private int categoryRender;
    private int categoryPlayer;
    private int categoryWorld;
    private boolean rainbow;
    private boolean animated;
    private float speed;
    private float glowIntensity;
    private float saturation;
    private float brightness;

    public ThemeColorPalette() {
        this.primary = new Color(89, 255, 231).getRGB();
        this.secondary = new Color(200, 80, 255).getRGB();
        this.tertiary = new Color(255, 105, 180).getRGB();
        this.background = new Color(20, 22, 28).getRGB();
        this.surface = new Color(30, 30, 40).getRGB();
        this.textPrimary = new Color(255, 255, 255).getRGB();
        this.textSecondary = new Color(131, 131, 131).getRGB();
        this.textAccent = new Color(200, 200, 255).getRGB();
        this.hudBackground = new Color(151, 142, 142, 255).getRGB();
        this.hudText = new Color(255, 255, 255).getRGB();
        this.hudAccent = new Color(89, 255, 231).getRGB();
        this.targetHudBackground = new Color(20, 25, 35, 255).getRGB();
        this.targetHudHealth = new Color(255, 105, 180).getRGB();
        this.targetHudAccent = new Color(89, 255, 231).getRGB();
        this.targetEspFill = new Color(255, 255, 255).getRGB();
        this.targetEspLine = new Color(89, 255, 231).getRGB();
        this.circleEsp = new Color(0, 255, 255).getRGB();
        this.spirits = new Color(0, 255, 255).getRGB();
        this.wingsPrimary = new Color(89, 255, 231).getRGB();
        this.wingsSecondary = ColorProvider.interpolateColor(new Color(89, 255, 231).getRGB(), Color.WHITE.getRGB(), 0.28f);
        this.wingsGlow = ColorProvider.interpolateColor(new Color(89, 255, 231).getRGB(), Color.WHITE.getRGB(), 0.55f);
        this.glow = new Color(255, 255, 255, 255).getRGB();
        this.blurColor = new Color(30, 30, 30, 255).getRGB();
        this.gradientStart = new Color(89, 255, 231).getRGB();
        this.gradientEnd = new Color(200, 80, 255).getRGB();
        this.progressBar = new Color(89, 255, 231).getRGB();
        this.progressBarBackground = new Color(50, 50, 50).getRGB();
        this.toggleOn = new Color(30, 180, 80).getRGB();
        this.toggleOff = new Color(200, 50, 50).getRGB();
        this.notificationSuccess = new Color(30, 180, 80).getRGB();
        this.notificationError = new Color(200, 50, 50).getRGB();
        this.notificationInfo = new Color(89, 255, 231).getRGB();
        this.nametagBackground = new Color(64, 64, 64, 255).getRGB();
        this.nametagHealth = new Color(255, 80, 80).getRGB();
        this.particlePrimary = new Color(150, 100, 200).getRGB();
        this.categoryCombat = new Color(255, 80, 80).getRGB();
        this.categoryMovement = new Color(80, 255, 80).getRGB();
        this.categoryRender = new Color(89, 255, 231).getRGB();
        this.categoryPlayer = new Color(255, 255, 80).getRGB();
        this.categoryWorld = new Color(200, 80, 255).getRGB();
        this.rainbow = false;
        this.animated = false;
        this.speed = 1.0f;
        this.glowIntensity = 1.0f;
        this.saturation = 1.0f;
        this.brightness = 1.0f;
    }

    public static ThemeColorPalette copyOf(ThemeColorPalette other) {
        ThemeColorPalette p = new ThemeColorPalette();
        p.primary = other.primary;
        p.secondary = other.secondary;
        p.tertiary = other.tertiary;
        p.background = other.background;
        p.surface = other.surface;
        p.textPrimary = other.textPrimary;
        p.textSecondary = other.textSecondary;
        p.textAccent = other.textAccent;
        p.hudBackground = other.hudBackground;
        p.hudText = other.hudText;
        p.hudAccent = other.hudAccent;
        p.targetHudBackground = other.targetHudBackground;
        p.targetHudHealth = other.targetHudHealth;
        p.targetHudAccent = other.targetHudAccent;
        p.targetEspFill = other.targetEspFill;
        p.targetEspLine = other.targetEspLine;
        p.circleEsp = other.circleEsp;
        p.spirits = other.spirits;
        p.wingsPrimary = other.wingsPrimary;
        p.wingsSecondary = other.wingsSecondary;
        p.wingsGlow = other.wingsGlow;
        p.glow = other.glow;
        p.blurColor = other.blurColor;
        p.gradientStart = other.gradientStart;
        p.gradientEnd = other.gradientEnd;
        p.progressBar = other.progressBar;
        p.progressBarBackground = other.progressBarBackground;
        p.toggleOn = other.toggleOn;
        p.toggleOff = other.toggleOff;
        p.notificationSuccess = other.notificationSuccess;
        p.notificationError = other.notificationError;
        p.notificationInfo = other.notificationInfo;
        p.nametagBackground = other.nametagBackground;
        p.nametagHealth = other.nametagHealth;
        p.particlePrimary = other.particlePrimary;
        p.categoryCombat = other.categoryCombat;
        p.categoryMovement = other.categoryMovement;
        p.categoryRender = other.categoryRender;
        p.categoryPlayer = other.categoryPlayer;
        p.categoryWorld = other.categoryWorld;
        p.rainbow = other.rainbow;
        p.animated = other.animated;
        p.speed = other.speed;
        p.glowIntensity = other.glowIntensity;
        p.saturation = other.saturation;
        p.brightness = other.brightness;
        return p;
    }

    public static ThemeColorPalette interpolate(ThemeColorPalette a, ThemeColorPalette b, float t) {
        ThemeColorPalette r = new ThemeColorPalette();
        r.primary = ColorProvider.interpolateColor(a.primary, b.primary, t);
        r.secondary = ColorProvider.interpolateColor(a.secondary, b.secondary, t);
        r.tertiary = ColorProvider.interpolateColor(a.tertiary, b.tertiary, t);
        r.background = ColorProvider.interpolateColor(a.background, b.background, t);
        r.surface = ColorProvider.interpolateColor(a.surface, b.surface, t);
        r.textPrimary = ColorProvider.interpolateColor(a.textPrimary, b.textPrimary, t);
        r.textSecondary = ColorProvider.interpolateColor(a.textSecondary, b.textSecondary, t);
        r.textAccent = ColorProvider.interpolateColor(a.textAccent, b.textAccent, t);
        r.hudBackground = ColorProvider.interpolateColor(a.hudBackground, b.hudBackground, t);
        r.hudText = ColorProvider.interpolateColor(a.hudText, b.hudText, t);
        r.hudAccent = ColorProvider.interpolateColor(a.hudAccent, b.hudAccent, t);
        r.targetHudBackground = ColorProvider.interpolateColor(a.targetHudBackground, b.targetHudBackground, t);
        r.targetHudHealth = ColorProvider.interpolateColor(a.targetHudHealth, b.targetHudHealth, t);
        r.targetHudAccent = ColorProvider.interpolateColor(a.targetHudAccent, b.targetHudAccent, t);
        r.targetEspFill = ColorProvider.interpolateColor(a.targetEspFill, b.targetEspFill, t);
        r.targetEspLine = ColorProvider.interpolateColor(a.targetEspLine, b.targetEspLine, t);
        r.circleEsp = ColorProvider.interpolateColor(a.circleEsp, b.circleEsp, t);
        r.spirits = ColorProvider.interpolateColor(a.spirits, b.spirits, t);
        r.wingsPrimary = ColorProvider.interpolateColor(a.wingsPrimary, b.wingsPrimary, t);
        r.wingsSecondary = ColorProvider.interpolateColor(a.wingsSecondary, b.wingsSecondary, t);
        r.wingsGlow = ColorProvider.interpolateColor(a.wingsGlow, b.wingsGlow, t);
        r.glow = ColorProvider.interpolateColor(a.glow, b.glow, t);
        r.blurColor = ColorProvider.interpolateColor(a.blurColor, b.blurColor, t);
        r.gradientStart = ColorProvider.interpolateColor(a.gradientStart, b.gradientStart, t);
        r.gradientEnd = ColorProvider.interpolateColor(a.gradientEnd, b.gradientEnd, t);
        r.progressBar = ColorProvider.interpolateColor(a.progressBar, b.progressBar, t);
        r.progressBarBackground = ColorProvider.interpolateColor(a.progressBarBackground, b.progressBarBackground, t);
        r.toggleOn = ColorProvider.interpolateColor(a.toggleOn, b.toggleOn, t);
        r.toggleOff = ColorProvider.interpolateColor(a.toggleOff, b.toggleOff, t);
        r.notificationSuccess = ColorProvider.interpolateColor(a.notificationSuccess, b.notificationSuccess, t);
        r.notificationError = ColorProvider.interpolateColor(a.notificationError, b.notificationError, t);
        r.notificationInfo = ColorProvider.interpolateColor(a.notificationInfo, b.notificationInfo, t);
        r.nametagBackground = ColorProvider.interpolateColor(a.nametagBackground, b.nametagBackground, t);
        r.nametagHealth = ColorProvider.interpolateColor(a.nametagHealth, b.nametagHealth, t);
        r.particlePrimary = ColorProvider.interpolateColor(a.particlePrimary, b.particlePrimary, t);
        r.categoryCombat = ColorProvider.interpolateColor(a.categoryCombat, b.categoryCombat, t);
        r.categoryMovement = ColorProvider.interpolateColor(a.categoryMovement, b.categoryMovement, t);
        r.categoryRender = ColorProvider.interpolateColor(a.categoryRender, b.categoryRender, t);
        r.categoryPlayer = ColorProvider.interpolateColor(a.categoryPlayer, b.categoryPlayer, t);
        r.categoryWorld = ColorProvider.interpolateColor(a.categoryWorld, b.categoryWorld, t);
        r.rainbow = t < 0.5f ? a.rainbow : b.rainbow;
        r.animated = t < 0.5f ? a.animated : b.animated;
        r.speed = a.speed + (b.speed - a.speed) * t;
        r.glowIntensity = a.glowIntensity + (b.glowIntensity - a.glowIntensity) * t;
        r.saturation = a.saturation + (b.saturation - a.saturation) * t;
        r.brightness = a.brightness + (b.brightness - a.brightness) * t;
        return r;
    }

    public void applyMultipliers() {
        if (saturation != 1f || brightness != 1f) {
            primary = adjustColor(primary, saturation, brightness);
            secondary = adjustColor(secondary, saturation, brightness);
            tertiary = adjustColor(tertiary, saturation, brightness);
            hudAccent = adjustColor(hudAccent, saturation, brightness);
            targetHudAccent = adjustColor(targetHudAccent, saturation, brightness);
            targetEspLine = adjustColor(targetEspLine, saturation, brightness);
            circleEsp = adjustColor(circleEsp, saturation, brightness);
            spirits = adjustColor(spirits, saturation, brightness);
            wingsPrimary = adjustColor(wingsPrimary, saturation, brightness);
            wingsSecondary = adjustColor(wingsSecondary, saturation, brightness);
            wingsGlow = adjustColor(wingsGlow, saturation, brightness);
            gradientStart = adjustColor(gradientStart, saturation, brightness);
            gradientEnd = adjustColor(gradientEnd, saturation, brightness);
            progressBar = adjustColor(progressBar, saturation, brightness);
            notificationInfo = adjustColor(notificationInfo, saturation, brightness);
            categoryRender = adjustColor(categoryRender, saturation, brightness);
            particlePrimary = adjustColor(particlePrimary, saturation, brightness);

        }
    }

    private static int adjustColor(int color, float saturation, float brightness) {
        float[] hsb = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        hsb[1] = Math.min(1f, hsb[1] * saturation);
        hsb[2] = Math.min(1f, hsb[2] * brightness);
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return (rgb & 0x00FFFFFF) | (color & 0xFF000000);
    }

    public int getCategoryColor(int categoryOrdinal) {
        switch (categoryOrdinal) {
            case 0:
                return categoryCombat;
            case 1:
                return categoryMovement;
            case 2:
                return categoryRender;
            case 3:
                return categoryPlayer;
            case 4:
                return categoryWorld;
            default:
                return primary;

        }
    }

    public int getPrimary() {
        return primary;
    }

    public void setPrimary(int primary) {
        this.primary = primary;
    }

    public int getSecondary() {
        return secondary;
    }

    public void setSecondary(int secondary) {
        this.secondary = secondary;
    }

    public int getTertiary() {
        return tertiary;
    }

    public void setTertiary(int tertiary) {
        this.tertiary = tertiary;
    }

    public int getBackground() {
        return background;
    }

    public void setBackground(int background) {
        this.background = background;
    }

    public int getSurface() {
        return surface;
    }

    public void setSurface(int surface) {
        this.surface = surface;
    }

    public int getTextPrimary() {
        return textPrimary;
    }

    public void setTextPrimary(int textPrimary) {
        this.textPrimary = textPrimary;
    }

    public int getTextSecondary() {
        return textSecondary;
    }

    public void setTextSecondary(int textSecondary) {
        this.textSecondary = textSecondary;
    }

    public int getTextAccent() {
        return textAccent;
    }

    public void setTextAccent(int textAccent) {
        this.textAccent = textAccent;
    }

    public int getHudBackground() {
        return hudBackground;
    }

    public void setHudBackground(int hudBackground) {
        this.hudBackground = hudBackground;
    }

    public int getHudText() {
        return hudText;
    }

    public void setHudText(int hudText) {
        this.hudText = hudText;
    }

    public int getHudAccent() {
        return hudAccent;
    }

    public void setHudAccent(int hudAccent) {
        this.hudAccent = hudAccent;
    }

    public int getTargetHudBackground() {
        return targetHudBackground;
    }

    public void setTargetHudBackground(int targetHudBackground) {
        this.targetHudBackground = targetHudBackground;
    }

    public int getTargetHudHealth() {
        return targetHudHealth;
    }

    public void setTargetHudHealth(int targetHudHealth) {
        this.targetHudHealth = targetHudHealth;
    }

    public int getTargetHudAccent() {
        return targetHudAccent;
    }

    public void setTargetHudAccent(int targetHudAccent) {
        this.targetHudAccent = targetHudAccent;
    }

    public int getTargetEspFill() {
        return targetEspFill;
    }

    public void setTargetEspFill(int targetEspFill) {
        this.targetEspFill = targetEspFill;
    }

    public int getTargetEspLine() {
        return targetEspLine;
    }

    public void setTargetEspLine(int targetEspLine) {
        this.targetEspLine = targetEspLine;
    }

    public int getCircleEsp() {
        return circleEsp;
    }

    public void setCircleEsp(int circleEsp) {
        this.circleEsp = circleEsp;
    }

    public int getSpirits() {
        return spirits;
    }

    public void setSpirits(int spirits) {
        this.spirits = spirits;
    }

    public int getWingsPrimary() {
        return wingsPrimary;
    }

    public void setWingsPrimary(int wingsPrimary) {
        this.wingsPrimary = wingsPrimary;
    }

    public int getWingsSecondary() {
        return wingsSecondary;
    }

    public void setWingsSecondary(int wingsSecondary) {
        this.wingsSecondary = wingsSecondary;
    }

    public int getWingsGlow() {
        return wingsGlow;
    }

    public void setWingsGlow(int wingsGlow) {
        this.wingsGlow = wingsGlow;
    }

    public int getGlow() {
        return glow;
    }

    public void setGlow(int glow) {
        this.glow = glow;
    }

    public int getBlurColor() {
        return blurColor;
    }

    public void setBlurColor(int blurColor) {
        this.blurColor = blurColor;
    }

    public int getGradientStart() {
        return gradientStart;
    }

    public void setGradientStart(int gradientStart) {
        this.gradientStart = gradientStart;
    }

    public int getGradientEnd() {
        return gradientEnd;
    }

    public void setGradientEnd(int gradientEnd) {
        this.gradientEnd = gradientEnd;
    }

    public int getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(int progressBar) {
        this.progressBar = progressBar;
    }

    public int getProgressBarBackground() {
        return progressBarBackground;
    }

    public void setProgressBarBackground(int progressBarBackground) {
        this.progressBarBackground = progressBarBackground;
    }

    public int getToggleOn() {
        return toggleOn;
    }

    public void setToggleOn(int toggleOn) {
        this.toggleOn = toggleOn;
    }

    public int getToggleOff() {
        return toggleOff;
    }

    public void setToggleOff(int toggleOff) {
        this.toggleOff = toggleOff;
    }

    public int getNotificationSuccess() {
        return notificationSuccess;
    }

    public void setNotificationSuccess(int notificationSuccess) {
        this.notificationSuccess = notificationSuccess;
    }

    public int getNotificationError() {
        return notificationError;
    }

    public void setNotificationError(int notificationError) {
        this.notificationError = notificationError;
    }

    public int getNotificationInfo() {
        return notificationInfo;
    }

    public void setNotificationInfo(int notificationInfo) {
        this.notificationInfo = notificationInfo;
    }

    public int getNametagBackground() {
        return nametagBackground;
    }

    public void setNametagBackground(int nametagBackground) {
        this.nametagBackground = nametagBackground;
    }

    public int getNametagHealth() {
        return nametagHealth;
    }

    public void setNametagHealth(int nametagHealth) {
        this.nametagHealth = nametagHealth;
    }

    public int getParticlePrimary() {
        return particlePrimary;
    }

    public void setParticlePrimary(int particlePrimary) {
        this.particlePrimary = particlePrimary;
    }

    public int getCategoryCombat() {
        return categoryCombat;
    }

    public void setCategoryCombat(int categoryCombat) {
        this.categoryCombat = categoryCombat;
    }

    public int getCategoryMovement() {
        return categoryMovement;
    }

    public void setCategoryMovement(int categoryMovement) {
        this.categoryMovement = categoryMovement;
    }

    public int getCategoryRender() {
        return categoryRender;
    }

    public void setCategoryRender(int categoryRender) {
        this.categoryRender = categoryRender;
    }

    public int getCategoryPlayer() {
        return categoryPlayer;
    }

    public void setCategoryPlayer(int categoryPlayer) {
        this.categoryPlayer = categoryPlayer;
    }

    public int getCategoryWorld() {
        return categoryWorld;
    }

    public void setCategoryWorld(int categoryWorld) {
        this.categoryWorld = categoryWorld;
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }

    public boolean isAnimated() {
        return animated;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getGlowIntensity() {
        return glowIntensity;
    }

    public void setGlowIntensity(float glowIntensity) {
        this.glowIntensity = glowIntensity;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;

    }
}


