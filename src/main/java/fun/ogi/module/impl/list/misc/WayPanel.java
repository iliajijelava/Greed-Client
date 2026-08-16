package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.KeySetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.screens.WaypointScreen;
import fun.ogi.util.storages.WaypointStorage;

@ModuleInformation(moduleName = "Way Panel",moduleDesc = "Gui for waypoints",moduleCategory = ModuleCategory.MISC)
public class WayPanel extends Module {
    private final KeySetting bind = new KeySetting("Menu opening bind",this,-1);
    private final SliderSetting size = new SliderSetting("Waypoint size",this,1.0,0.5,3.0,0.1);
    private boolean wasPressed;
    public WayPanel(){
        addSettings(bind, size);
    }
    @Subscribe
    private void onUpdate(EventUpdate e){
        if (mc.player==null || mc.world==null)return;
        WaypointStorage.waypointScale = size.getFloatValue();
        boolean pressed = bind.isPressed();
        if (pressed && !wasPressed) mc.setScreen(new WaypointScreen(mc.currentScreen));
        wasPressed = pressed;
    }
}

