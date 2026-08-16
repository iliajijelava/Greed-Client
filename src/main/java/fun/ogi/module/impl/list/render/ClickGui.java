package fun.ogi.module.impl.list.render;


import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.impl.list.misc.ClientSounds;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.screens.ClickGuiScreen;
import fun.ogi.screens.DropDownGui;
import fun.ogi.screens.SecondClickGuiScreen;
import org.lwjgl.glfw.GLFW;

import static fun.ogi.util.MinecraftUtil.mc;

@ModuleInformation(moduleName = "ClickGUI", moduleDesc = "Opens the ClickGUI.", moduleCategory = ModuleCategory.RENDER, moduleKeybind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGui extends Module {

    public final SliderSetting guiScale = new SliderSetting("GUI Scale", this, 1.0, 0.5, 2.0, 0.05);
    public final ModeSetting guiStyle = new ModeSetting("Gui Style ", this,"Second","Second", "Default", "Dropdown");
    public final ModeSetting builderStyle = new ModeSetting("Builder Style", this, "Blur", "Default", "Blur");
    public ClickGui() {
        addSettings(guiStyle, builderStyle);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            ClientSounds.INSTANCE.playOpenGui();
            String style = guiStyle.getValueAsString();
            if(style.equals("Default")){
                mc.setScreen(new ClickGuiScreen((float) guiScale.getValue()));
            } else if(style.equals("Second")){
                mc.setScreen(new SecondClickGuiScreen((float) guiScale.getValue(), builderStyle.getValueAsString()));
            } else if(style.equals("Dropdown")){
                mc.setScreen(new DropDownGui(guiScale.getFloatValue()));
            }
        }
        setEnabled(false);
    }
}

