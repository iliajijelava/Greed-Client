package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.EventKeyboard;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.KeySetting;

@ModuleInformation(moduleName = "Help Messages",moduleDesc = "Sends into global your coordinates message",moduleCategory = ModuleCategory.PLAYER)
public class HelpMessage extends Module {
    private final KeySetting bind = new KeySetting("Send Bind",this,-1);
    public HelpMessage(){
        addSetting(bind);
    }
    @Subscribe
    private void onKey(EventKeyboard e){
        if(mc.player==null || mc.world == null)return;
        int x = mc.player.getBlockX();
        int y = mc.player.getBlockY();
        int z = mc.player.getBlockZ();
        if(bind.isPressed()) mc.getNetworkHandler().sendChatCommand("! Помогите бахните трапку " + x + y + z + "дам награду!");
    }
}

