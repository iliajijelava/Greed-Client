package fun.ogi.module;

import fun.ogi.Cheap;
import fun.ogi.events.ModuleToggleEvent;
import fun.ogi.module.settings.Setting;
import fun.ogi.util.NotificationManager;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class Module {
    private final String name, desc;
    private final ModuleCategory category;
    private int keybind;
    private boolean enabled;

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<Setting> settings = new ArrayList<>();

    public Module() {
        ModuleInformation information = getClass().getAnnotation(ModuleInformation.class);

        this.name = information.moduleName();
        this.desc = information.moduleDesc();
        this.category = information.moduleCategory();
        this.keybind = information.moduleKeybind();
    }

    public List<Setting> getSettings() {
        return settings;
    }

    public void addSetting(Setting setting) {
        settings.add(setting);
    }

    public void addSettings(Setting... settings) {
        for (Setting setting : settings) {
            addSetting(setting);
        }
    }

    public Setting getSettingByName(String name) {
        for (Setting setting : settings) {
            if (setting.getName().equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    public String getDesc() {
        return desc;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public int getKeybind() {
        return keybind;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;

            NotificationManager.post(name + (enabled ? " Enabled" : " Disabled"), NotificationManager.TYPE_INFO, 2500, enabled ? 'J' : 'K');

            Cheap.getInstance().getEventBus().post(new ModuleToggleEvent(this, enabled));

            if (enabled) {
                Cheap.getInstance().getEventBus().register(this);
                onEnable();
            } else {
                Cheap.getInstance().getEventBus().unregister(this);
                onDisable();
            }
        }
    }

    public void onEnable() {}

    public void onDisable() {}

    public void toggle() {
        setEnabled(!isEnabled());
    }

}

