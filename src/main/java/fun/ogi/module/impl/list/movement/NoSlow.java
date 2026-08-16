package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.player.EventSlowWalking;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

@ModuleInformation(moduleName = "No Slow", moduleDesc = "Removes slowness when eating or using shield", moduleCategory = ModuleCategory.MOVEMENT)
public class NoSlow extends Module {

    public static NoSlow INSTANCE = new NoSlow();
    private int ticks;
    private final ModeSetting mode = new ModeSetting("Mode: ",this, "Grim", "Spooky", "HolyWorld");
    private final BooleanSetting sprint = new BooleanSetting("Sprint",this, true);

    public NoSlow() {

        addSettings(mode, sprint);
    }

    @Subscribe
    public void onSlowDown(EventSlowWalking event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (!mc.player.isUsingItem()) {
            this.ticks = 0;
            return;
        }
        if(!sprint.getValue()){
            mc.player.setSprinting(false);
        }
        String currentMode = this.mode.getValueAsString();

        if (currentMode.equals("Spooky") || currentMode.equals("HolyWorld")) {
            this.ticks++;
        }

        mc.player.setSprinting(true);

        Hand activeHand = mc.player.getActiveHand();
        Hand oppositeHand = activeHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;

        if (!currentMode.equals("Spooky")) {
            mc.interactionManager
                    .sendSequencedPacket(
                            mc.world, sequence -> new PlayerInteractItemC2SPacket(oppositeHand, sequence, mc.player.getYaw(), mc.player.getPitch())
                    );
        }

        if (currentMode.equals("Grim") || this.ticks >= 2 || currentMode.equals("HolyWorld")) {
            event.cancel();
            this.ticks = 0;
        }
    }

}

