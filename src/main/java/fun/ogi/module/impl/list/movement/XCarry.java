package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.PacketEvent;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

@ModuleInformation(moduleName = "XCarry", moduleDesc = "More Slots", moduleCategory = ModuleCategory.MISC)
public class XCarry extends Module {

    public static XCarry INSTANCE = new XCarry();
    public BooleanSetting autoDisable = new BooleanSetting("Auto disable", this,true);

    public XCarry(){
        addSetting(autoDisable);
    }
    private boolean wasInInventory = false;
    @Subscribe
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof CloseHandledScreenC2SPacket && mc.currentScreen instanceof InventoryScreen) {
            event.cancelEvent();
            wasInInventory = true;
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (wasInInventory && mc.currentScreen == null) {
            if (autoDisable.getValue()) {
                toggle();
            }
            wasInInventory = false;
        }
    }
}

