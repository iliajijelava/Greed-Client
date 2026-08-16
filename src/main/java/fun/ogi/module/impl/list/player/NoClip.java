package fun.ogi.module.impl.list.player;


import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;

@ModuleInformation(moduleName = "NoClip",moduleDesc = "Lets you going through blocks (Xuinya)",moduleCategory = ModuleCategory.PLAYER)
public class NoClip extends Module {

    public static NoClip INSTANCE = new NoClip();
    public NoClip() {

    }

    
    @Subscribe
    public void onUpdate(final EventUpdate ignored) {
        if (mc.player == null) return;

        if (mc.player.age % 35 == 0) {
            mc.player.networkHandler.sendChatMessage("/gmsp");
        } else if (mc.player.age % 35 == 2){
            mc.player.networkHandler.sendChatMessage("/gms");
        }
    }
}


