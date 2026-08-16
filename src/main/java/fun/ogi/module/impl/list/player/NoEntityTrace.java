package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.AttackEvent;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import net.minecraft.item.SwordItem;

@ModuleInformation(moduleName = "NoEntityTrace", moduleDesc = "Ignores entity interaction when mining", moduleCategory = ModuleCategory.PLAYER)
public class NoEntityTrace extends Module {

    private final BooleanSetting noSword = new BooleanSetting("No Sword", this, true);

    public NoEntityTrace() {
        addSetting(noSword);
    }

    public boolean shouldIgnoreEntityTrace() {
        if (mc.player == null) return false;
        return isEnabled() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem && noSword.getValue());
    }

    @Subscribe
    public void onAttack(AttackEvent e) {
        if (shouldIgnoreEntityTrace()) {
            e.cancelEvent();
        }
    }
}

