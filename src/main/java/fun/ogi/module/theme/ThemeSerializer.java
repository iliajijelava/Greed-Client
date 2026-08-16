package fun.ogi.module.theme;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;

import java.awt.Color;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ThemeSerializer {
    private final File themeDir;
    private final Gson gson;

    public ThemeSerializer() {
        this.themeDir = new File(MinecraftClient.getInstance().runDirectory, "ExortWare/themes");
        if (!themeDir.exists()) {
            themeDir.mkdirs();
        }
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public File getThemeDir() {
        return themeDir;
    }

    public void saveTheme(Theme theme) {
        File file = new File(themeDir, sanitizeFileName(theme.getName()) + ".json");
        JsonObject root = themeToJson(theme);
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public Theme loadTheme(String name) {
        File file = new File(themeDir, sanitizeFileName(name) + ".json");
        return loadThemeFromFile(file);
    }

    public Theme loadThemeFromFile(File file) {
        if (!file.exists()) return null;
        try (Reader reader = new FileReader(file)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) return null;
            return jsonToTheme(element.getAsJsonObject());
        } catch (IOException e) {
            e.printStackTrace();
            return null;

        }
    }

    public List<Theme> loadAllCustomThemes() {
        List<Theme> themes = new ArrayList<>();
        File[] files = themeDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return themes;
        for (File file : files) {
            Theme theme = loadThemeFromFile(file);
            if (theme != null) {
                theme.setCustom(true);
                themes.add(theme);

            }
        }
        return themes;
    }

    public void deleteTheme(String name) {
        File file = new File(themeDir, sanitizeFileName(name) + ".json");
        if (file.exists()) file.delete();
    }

    public boolean themeExists(String name) {
        return new File(themeDir, sanitizeFileName(name) + ".json").exists();
    }

    public String exportThemeAsJson(Theme theme) {
        return gson.toJson(themeToJson(theme));
    }

    public Theme importThemeFromJson(String jsonString) {
        try {
            JsonElement element = JsonParser.parseString(jsonString);
            if (element == null || !element.isJsonObject()) return null;
            return jsonToTheme(element.getAsJsonObject());
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;

        }
    }

    private JsonObject themeToJson(Theme theme) {
        JsonObject root = new JsonObject();
        root.addProperty("name", theme.getName());
        root.addProperty("description", theme.getDescription());
        root.addProperty("favorite", theme.isFavorite());
        root.add("palette", paletteToJson(theme.getPalette()));
        return root;
    }

    private JsonObject paletteToJson(ThemeColorPalette p) {
        JsonObject j = new JsonObject();
        writeColor(j, "primary", p.getPrimary());
        writeColor(j, "secondary", p.getSecondary());
        writeColor(j, "tertiary", p.getTertiary());
        writeColor(j, "background", p.getBackground());
        writeColor(j, "surface", p.getSurface());
        writeColor(j, "textPrimary", p.getTextPrimary());
        writeColor(j, "textSecondary", p.getTextSecondary());
        writeColor(j, "textAccent", p.getTextAccent());
        writeColor(j, "hudBackground", p.getHudBackground());
        writeColor(j, "hudText", p.getHudText());
        writeColor(j, "hudAccent", p.getHudAccent());
        writeColor(j, "targetHudBackground", p.getTargetHudBackground());
        writeColor(j, "targetHudHealth", p.getTargetHudHealth());
        writeColor(j, "targetHudAccent", p.getTargetHudAccent());
        writeColor(j, "targetEspFill", p.getTargetEspFill());
        writeColor(j, "targetEspLine", p.getTargetEspLine());
        writeColor(j, "circleEsp", p.getCircleEsp());
        writeColor(j, "spirits", p.getSpirits());
        writeColor(j, "wingsPrimary", p.getWingsPrimary());
        writeColor(j, "wingsSecondary", p.getWingsSecondary());
        writeColor(j, "wingsGlow", p.getWingsGlow());
        writeColor(j, "glow", p.getGlow());
        writeColor(j, "blurColor", p.getBlurColor());
        writeColor(j, "gradientStart", p.getGradientStart());
        writeColor(j, "gradientEnd", p.getGradientEnd());
        writeColor(j, "progressBar", p.getProgressBar());
        writeColor(j, "progressBarBackground", p.getProgressBarBackground());
        writeColor(j, "toggleOn", p.getToggleOn());
        writeColor(j, "toggleOff", p.getToggleOff());
        writeColor(j, "notificationSuccess", p.getNotificationSuccess());
        writeColor(j, "notificationError", p.getNotificationError());
        writeColor(j, "notificationInfo", p.getNotificationInfo());
        writeColor(j, "nametagBackground", p.getNametagBackground());
        writeColor(j, "nametagHealth", p.getNametagHealth());
        writeColor(j, "particlePrimary", p.getParticlePrimary());
        writeColor(j, "categoryCombat", p.getCategoryCombat());
        writeColor(j, "categoryMovement", p.getCategoryMovement());
        writeColor(j, "categoryRender", p.getCategoryRender());
        writeColor(j, "categoryPlayer", p.getCategoryPlayer());
        writeColor(j, "categoryWorld", p.getCategoryWorld());
        j.addProperty("rainbow", p.isRainbow());
        j.addProperty("animated", p.isAnimated());
        j.addProperty("speed", p.getSpeed());
        j.addProperty("glowIntensity", p.getGlowIntensity());
        j.addProperty("saturation", p.getSaturation());
        j.addProperty("brightness", p.getBrightness());
        return j;
    }

    private Theme jsonToTheme(JsonObject j) {
        String name = j.get("name").getAsString();
        String description = j.has("description") ? j.get("description").getAsString() : "";
        boolean favorite = j.has("favorite") && j.get("favorite").getAsBoolean();
        ThemeColorPalette p = new ThemeColorPalette();
        JsonObject pal = j.getAsJsonObject("palette");
        if (pal != null) {
            readColor(pal, "primary", p::setPrimary);
            readColor(pal, "secondary", p::setSecondary);
            readColor(pal, "tertiary", p::setTertiary);
            readColor(pal, "background", p::setBackground);
            readColor(pal, "surface", p::setSurface);
            readColor(pal, "textPrimary", p::setTextPrimary);
            readColor(pal, "textSecondary", p::setTextSecondary);
            readColor(pal, "textAccent", p::setTextAccent);
            readColor(pal, "hudBackground", p::setHudBackground);
            readColor(pal, "hudText", p::setHudText);
            readColor(pal, "hudAccent", p::setHudAccent);
            readColor(pal, "targetHudBackground", p::setTargetHudBackground);
            readColor(pal, "targetHudHealth", p::setTargetHudHealth);
            readColor(pal, "targetHudAccent", p::setTargetHudAccent);
            readColor(pal, "targetEspFill", p::setTargetEspFill);
            readColor(pal, "targetEspLine", p::setTargetEspLine);
            readColor(pal, "circleEsp", p::setCircleEsp);
            readColor(pal, "spirits", p::setSpirits);
            readColor(pal, "wingsPrimary", p::setWingsPrimary);
            readColor(pal, "wingsSecondary", p::setWingsSecondary);
            readColor(pal, "wingsGlow", p::setWingsGlow);
            readColor(pal, "glow", p::setGlow);
            readColor(pal, "blurColor", p::setBlurColor);
            readColor(pal, "gradientStart", p::setGradientStart);
            readColor(pal, "gradientEnd", p::setGradientEnd);
            readColor(pal, "progressBar", p::setProgressBar);
            readColor(pal, "progressBarBackground", p::setProgressBarBackground);
            readColor(pal, "toggleOn", p::setToggleOn);
            readColor(pal, "toggleOff", p::setToggleOff);
            readColor(pal, "notificationSuccess", p::setNotificationSuccess);
            readColor(pal, "notificationError", p::setNotificationError);
            readColor(pal, "notificationInfo", p::setNotificationInfo);
            readColor(pal, "nametagBackground", p::setNametagBackground);
            readColor(pal, "nametagHealth", p::setNametagHealth);
            readColor(pal, "particlePrimary", p::setParticlePrimary);
            readColor(pal, "categoryCombat", p::setCategoryCombat);
            readColor(pal, "categoryMovement", p::setCategoryMovement);
            readColor(pal, "categoryRender", p::setCategoryRender);
            readColor(pal, "categoryPlayer", p::setCategoryPlayer);
            readColor(pal, "categoryWorld", p::setCategoryWorld);
            if (pal.has("rainbow")) p.setRainbow(pal.get("rainbow").getAsBoolean());
            if (pal.has("animated")) p.setAnimated(pal.get("animated").getAsBoolean());
            if (pal.has("speed")) p.setSpeed(pal.get("speed").getAsFloat());
            if (pal.has("glowIntensity")) p.setGlowIntensity(pal.get("glowIntensity").getAsFloat());
            if (pal.has("saturation")) p.setSaturation(pal.get("saturation").getAsFloat());
            if (pal.has("brightness")) p.setBrightness(pal.get("brightness").getAsFloat());
        }
        Theme theme = new Theme(name, description, p);
        theme.setFavorite(favorite);
        theme.setCustom(true);
        return theme;
    }

    private void writeColor(JsonObject j, String key, int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        JsonObject c = new JsonObject();
        c.addProperty("r", r);
        c.addProperty("g", g);
        c.addProperty("b", b);
        c.addProperty("a", a);
        j.add(key, c);
    }

    private void readColor(JsonObject j, String key, java.util.function.IntConsumer setter) {
        if (!j.has(key)) return;
        JsonElement el = j.get(key);
        if (el.isJsonObject()) {
            JsonObject c = el.getAsJsonObject();
            int r = c.get("r").getAsInt();
            int g = c.get("g").getAsInt();
            int b = c.get("b").getAsInt();
            int a = c.has("a") ? c.get("a").getAsInt() : 255;
            setter.accept((a << 24) | (r << 16) | (g << 8) | b);
        } else if (el.isJsonPrimitive()) {
            setter.accept(el.getAsInt());

        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");

    }
}

