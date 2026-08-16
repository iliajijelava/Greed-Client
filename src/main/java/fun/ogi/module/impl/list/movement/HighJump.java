package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;

@ModuleInformation(moduleName = "HighJump", moduleCategory = ModuleCategory.MOVEMENT)
public class HighJump extends Module {
    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;
        if (mc.player.age % 4 == 0) {
            mc.player.jump();
            mc.player.setVelocity(0, 3, 0);
            toggle();
        }
    }
}

