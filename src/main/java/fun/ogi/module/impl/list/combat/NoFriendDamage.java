package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.AttackEvent;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;

@ModuleInformation(moduleName = "NoFriendDamage", moduleDesc = "Prevents attacking friends", moduleCategory = ModuleCategory.COMBAT)
public class NoFriendDamage extends Module {

    @Subscribe
    public void onAttack(AttackEvent e) {
        if (Cheap.getInstance().getFriendManager().contains(e.getEntity().getName().getString())) {
            e.cancelEvent();
        }
    }
}

