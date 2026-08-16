package fun.ogi.module.impl.list.player;


import com.google.common.eventbus.Subscribe;
import fun.ogi.events.EventKeyboard;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.KeySetting;

@ModuleInformation(moduleName = "KTLeave", moduleDesc = "Quick teleport away on key press", moduleCategory = ModuleCategory.PLAYER)
public class KTLeave extends Module {

    public static KTLeave INSTANCE = new KTLeave();

    private boolean hasGM;
    private double lastX, lastY, lastZ;

    private KeySetting bind = new KeySetting("Leave button",this, -1);

    public KTLeave() {
        addSettings(bind);
    }

    
    @Subscribe
    public void onKey(final EventKeyboard e) {
        if (mc.player == null) return;
        if (e.getKeyCode() == bind.getKey()) {
            hasGM = !hasGM;

            if (hasGM) {
                lastX = mc.player.getX();
                lastY = mc.player.getY();
                lastZ = mc.player.getZ();
                mc.player.setPosition(mc.player.getX() + 10, mc.player.getY() + 10, mc.player.getZ() + 10);
            } else {
                mc.player.setPosition(lastX, lastY, lastZ);
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        hasGM = false;
    }
}

