package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;

@ModuleInformation(moduleName = "AutoRespawn", moduleDesc = "Automatically respawns on death", moduleCategory = ModuleCategory.PLAYER)
public class AutoRespawn extends Module {

    @Subscribe
    public void onEventUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isDead()) return;

        mc.player.requestRespawn();
        mc.setScreen(null);
    }
}

