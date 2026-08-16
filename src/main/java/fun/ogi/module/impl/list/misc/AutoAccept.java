package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.PacketEvent;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import java.util.Locale;

@ModuleInformation(moduleName = "Auto TPA Accept", moduleDesc = "Automatically accepts TPA", moduleCategory = ModuleCategory.MISC)
public class AutoAccept extends Module {

    public static AutoAccept INSTANCE = new AutoAccept();

    private final BooleanSetting onlyFriend = new BooleanSetting("Only friend",this ,false);

    public AutoAccept() {
        addSettings(onlyFriend);
    }

    @Subscribe
    public void onEvent(final PacketEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getType() != PacketEvent.Type.RECEIVE) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof GameMessageS2CPacket messagePacket) {
            String raw = messagePacket.content().getString().toLowerCase(Locale.ROOT);

            if (raw.contains("телепортироваться") || raw.contains("has requested teleport") || raw.contains("просит к вам телепортироваться")) {
                if (onlyFriend.getValue()) {
                    boolean isFriend = false;

                    if (Cheap.getInstance().getFriendManager() != null) {
                        for (String friend : Cheap.getInstance().getFriendManager().getFriends()) {
                            if (raw.contains(friend.toLowerCase(Locale.ROOT))) {
                                isFriend = true;
                                break;
                            }
                        }
                    }

                    if (!isFriend) {
                        return;
                    }
                }

                mc.player.networkHandler.sendChatCommand("tpaccept");
            }
        }
    }
}

