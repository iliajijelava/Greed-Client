package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventSwingDuration;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.NumberSetting;

@ModuleInformation(moduleName = "SwingAnimations", moduleDesc = "Custom attack animation", moduleCategory = ModuleCategory.RENDER)
public class SwingAnimations extends Module {
    public static SwingAnimations INSTANCE;

    public final BooleanSetting swingEnabled = new BooleanSetting("Swing Enabled", this, true);

    public final ModeSetting swingType = new ModeSetting("Swing Type", this, "Smooth",
            "Smooth", "Static", "Down", "DropDown", "Poke", "SelfBack",
            "Feast", "ToBack", "Block", "Akrien", "Break", "Pander", "Slant","HMI");

    public final NumberSetting swingStrength = new NumberSetting("Swing Strength", this, 1.0, 0.1, 3.0, 0.01);

    public final NumberSetting corner = new NumberSetting("DropDown Corner", this, 12.0, 1.0, 360.0, 1.0);

    public final NumberSetting slant = new NumberSetting("DropDown Slant", this, 12.0, 1.0, 360.0, 1.0);

    public final BooleanSetting auraTargetOnly = new BooleanSetting("Aura Only", this, false);

    public final BooleanSetting swapHands = new BooleanSetting("Swap Hands", this, false);

    public final BooleanSetting eatAnimation = new BooleanSetting("Eat Animation", this, false);

    public final BooleanSetting smoothEnabled = new BooleanSetting("Smooth Animation", this, false);

    public final NumberSetting smoothSpeed = new NumberSetting("Smooth Speed", this, 12.0, 1.0, 50.0, 1.0);

    public SwingAnimations() {
        INSTANCE = this;
        addSetting(swingEnabled);
        addSetting(swingType);
        addSetting(swingStrength);
        addSetting(corner);
        addSetting(slant);
        addSetting(auraTargetOnly);
        addSetting(swapHands);
        addSetting(eatAnimation);
        addSetting(smoothEnabled);
        addSetting(smoothSpeed);
    }

    @Subscribe
    public void onSwingDuration(EventSwingDuration event) {
        if (smoothEnabled.getValue()) {
            float original = event.getDuration();
            float speed = smoothSpeed.getFloatValue();
            event.setDuration(original * (12.0f / speed));
        }
    }
}

