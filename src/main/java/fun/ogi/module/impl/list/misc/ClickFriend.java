package fun.ogi.module.impl.list.misc;


import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.EventKeyboard;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.KeySetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

@ModuleInformation(moduleName = "Click friend",moduleDesc = "Adds player to friend list by clicking on bind",moduleCategory = ModuleCategory.MISC)
public class ClickFriend extends Module {

    private final KeySetting bind = new KeySetting("Bind", this,-1);

    public ClickFriend() {
        addSetting(bind);

    }
    @Subscribe
    public void onUpdate(EventUpdate e) {
        var fm = Cheap.getInstance().getFriendManager();
        if (bind.isPressed() && mc.crosshairTarget instanceof EntityHitResult result && result.getEntity() instanceof PlayerEntity player) {
            if (fm.getFriends().contains(player.getName().getString())) fm.remove(player.getName().getString());
            else fm.add(player.getName().getString());
        }
    }
}