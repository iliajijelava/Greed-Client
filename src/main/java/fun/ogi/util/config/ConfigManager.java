package fun.ogi.util.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.ogi.Cheap;
import fun.ogi.module.Module;
import fun.ogi.module.impl.list.render.Hud;
import fun.ogi.module.settings.Setting;
import fun.ogi.module.theme.Theme;
import fun.ogi.module.theme.ThemeManager;
import fun.ogi.util.NotificationManager;
import fun.ogi.util.render.Draggable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File configDir;

    public ConfigManager() {
        File nortiDir = Cheap.getInstance().getCheapDir();
        this.configDir = new File(nortiDir, "configs");
        if (!configDir.exists()) configDir.mkdirs();
    }

    public void saveConfig(String name) {
        JsonObject root = new JsonObject();
        for (fun.ogi.module.Module module : Cheap.getInstance().getModuleStorage().getModules()) {
            JsonObject modObj = new JsonObject();
            modObj.addProperty("enabled", module.isEnabled());
            modObj.addProperty("keybind", module.getKeybind());

            JsonObject settingsObj = new JsonObject();
            for (Setting setting : module.getSettings()) {
                settingsObj.addProperty(setting.getName(), setting.getValueAsString());
            }
            if (settingsObj.size() > 0) {
                modObj.add("settings", settingsObj);
            }
            root.add(module.getName(), modObj);
        }

        Theme currentTheme = ThemeManager.getInstance().getCurrentTheme();
        if (currentTheme != null) {
            root.addProperty("_theme", currentTheme.getName());
        }

        JsonArray macros = new JsonArray();
        for (fun.ogi.util.macro.Macro macro : fun.ogi.util.macro.MacroManager.getMacros()) {
            JsonObject macroObj = new JsonObject();
            macroObj.addProperty("command", macro.getCommand());
            macroObj.addProperty("key", macro.getKey());
            macros.add(macroObj);
        }
        root.add("_macros", macros);

        JsonObject positions = new JsonObject();
        Hud hud = Cheap.getInstance().getModuleStorage().get(Hud.class);
        if (hud != null) {
            addPosition(positions, "potions", hud.potionsDrag);
            addPosition(positions, "keybinds", hud.keybindsDrag);
            addPosition(positions, "armor", hud.armorDrag);
            addPosition(positions, "cooldowns", hud.cooldownsDrag);
            addPosition(positions, "targetHud", hud.targetHudComp.draggable);
            addPosition(positions, "staffList", hud.staffListComp.draggable);
            addPosition(positions, "moduleList", hud.moduleListComp.draggable);
        }
        addPosition(positions, "notifications", NotificationManager.draggable);
        root.add("_positions", positions);

        try (FileWriter writer = new FileWriter(new File(configDir, name + ".json"))) {
            GSON.toJson(root, writer);
        } catch (Exception ignored) {}
    }

    public void loadConfig(String name) {
        File file = new File(configDir, name + ".json");
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (Module module : Cheap.getInstance().getModuleStorage().getModules()) {
                if (!root.has(module.getName())) continue;
                JsonObject modObj = root.getAsJsonObject(module.getName());

                if (modObj.has("enabled")) {
                    module.setEnabled(modObj.get("enabled").getAsBoolean());
                }
                if (modObj.has("keybind")) {
                    module.setKeybind(modObj.get("keybind").getAsInt());
                }

                if (modObj.has("settings")) {
                    JsonObject settingsObj = modObj.getAsJsonObject("settings");
                    for (Setting setting : module.getSettings()) {
                        if (settingsObj.has(setting.getName())) {
                            setting.setValueFromString(settingsObj.get(setting.getName()).getAsString());
                        }
                    }
                }
            }

            if (root.has("_theme")) {
                String themeName = root.get("_theme").getAsString();
                Theme theme = ThemeManager.getInstance().getThemeByName(themeName);
                if (theme != null) {
                    ThemeManager.getInstance().setTheme(theme);
                }
            }

            if (root.has("_macros")) {
                fun.ogi.util.macro.MacroManager.clear();
                JsonArray macros = root.getAsJsonArray("_macros");
                for (JsonElement element : macros) {
                    JsonObject macroObj = element.getAsJsonObject();
                    String command = macroObj.get("command").getAsString();
                    int key = macroObj.get("key").getAsInt();
                    fun.ogi.util.macro.MacroManager.add(new fun.ogi.util.macro.Macro(command, key));
                }
            }

            if (root.has("_positions")) {
                JsonObject positions = root.getAsJsonObject("_positions");
                Hud hud = Cheap.getInstance().getModuleStorage().get(Hud.class);
                if (hud != null) {
                    loadPosition(positions, "potions", hud.potionsDrag);
                    loadPosition(positions, "keybinds", hud.keybindsDrag);
                    loadPosition(positions, "armor", hud.armorDrag);
                    loadPosition(positions, "cooldowns", hud.cooldownsDrag);
                    loadPosition(positions, "targetHud", hud.targetHudComp.draggable);
                    loadPosition(positions, "staffList", hud.staffListComp.draggable);
                    loadPosition(positions, "moduleList", hud.moduleListComp.draggable);
                }
                loadPosition(positions, "notifications", NotificationManager.draggable);
            }
        } catch (Exception ignored) {}
    }

    private void addPosition(JsonObject obj, String name, Draggable d) {
        JsonObject pos = new JsonObject();
        pos.addProperty("x", d.getX());
        pos.addProperty("y", d.getY());
        obj.add(name, pos);
    }

    private void loadPosition(JsonObject obj, String name, Draggable d) {
        if (obj.has(name)) {
            JsonObject pos = obj.getAsJsonObject(name);
            d.setX(pos.get("x").getAsFloat());
            d.setY(pos.get("y").getAsFloat());
        }
    }

    public void deleteConfig(String name) {
        if (name.equals("default")) return;
        File file = new File(configDir, name + ".json");
        if (file.exists()) file.delete();
    }

    public void clearConfigs() {
        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json") && !name.equals("default.json"));
        if (files != null) {
            for (File f : files) f.delete();
        }
    }

    public String[] listConfigs() {
        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return new String[0];
        return Arrays.stream(files)
                .map(f -> f.getName().replace(".json", ""))
                .toArray(String[]::new);
    }

    public File getConfigDir() {
        return configDir;
    }
}

