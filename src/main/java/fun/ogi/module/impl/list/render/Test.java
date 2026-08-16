package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.KeySetting;
import fun.ogi.screens.ClickGuiScreen;
import fun.ogi.screens.FiguraModelsGuiComponent;

@ModuleInformation(moduleName = "Test", moduleDesc = "Test", moduleCategory = ModuleCategory.RENDER)
public class Test extends Module {
    private final KeySetting bind = new KeySetting("Open Key", this, -1);
    private boolean guiOpen = false;
    private boolean prevKeyDown = false;

    public Test() {
        addSettings(bind);
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        boolean keyDown = bind.isKeyDown();

        if (keyDown && !prevKeyDown) {
            if (mc.currentScreen == null) {
                mc.setScreen(new FiguraModelsGuiComponent());
                guiOpen = true;
            }
        }

        prevKeyDown = keyDown;
    }
}


