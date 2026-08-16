package fun.ogi.module.impl.list.render;

import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ListSetting;

@ModuleInformation(moduleName = "Removals", moduleDesc = "Rename of No Render", moduleCategory = ModuleCategory.RENDER)
public class Removals extends Module {

    private final ListSetting elements = new ListSetting("Elements", this,
            "Fire", "Bad Effects", "Totem Animation", "Pumpkin", "Hurt Camera", "Scoreboard", "Boss Bar", "Overlay in block");

    public Removals() {
        addSettings(elements);
    }

    public boolean isEnabled(String element) {
        return isEnabled() && elements.isSelected(element);
    }

    public boolean isTotemAnimationDisabled() {
        return isEnabled("Totem Animation");
    }
}

