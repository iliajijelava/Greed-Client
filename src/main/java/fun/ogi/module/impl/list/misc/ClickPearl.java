package fun.ogi.module.impl.list.misc;


import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.KeySetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.NotificationManager;
import fun.ogi.util.StopWatch;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

@ModuleInformation(moduleName = "Click Pearl", moduleCategory = ModuleCategory.MISC)
public class ClickPearl extends Module {
    private final KeySetting bind = new KeySetting("Bind", this, -1);
    private final BooleanSetting legit = new BooleanSetting("Legit Pearl", this, true);
    private final SliderSetting throwTicksSetting = new SliderSetting("Throw Ticks", this, 2, 0, 20, 1);
    private final BooleanSetting swapRender = new BooleanSetting("Swap Render", this, true);
    private boolean waitingForThrow;
    private int currentThrowTicks;
    private int oldSlot;
    private final StopWatch delay = new StopWatch();

    public ClickPearl() {
        addSetting(bind);
        addSetting(legit);
        addSetting(throwTicksSetting);
        addSetting(swapRender);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (waitingForThrow) {
            currentThrowTicks--;
            if (currentThrowTicks <= 0) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.getInventory().selectedSlot = oldSlot;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                if (swapRender.getValue()) {
                    NotificationManager.post("Click Pearl: Pearl used");
                }
                waitingForThrow = false;
            }
            return;
        }
        if (!delay.hasTimePassed(500)) return;

        if (mc.currentScreen == null && bind.justPressed()) {
            int slot = findPearl();
            if (slot == -1) return;
            if (legit.getValue()) {

                if (slot < 9) {
                    oldSlot = mc.player.getInventory().selectedSlot;

                    mc.player.getInventory().selectedSlot = slot;
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                    currentThrowTicks = throwTicksSetting.getIntValue();
                    waitingForThrow = true;
                    delay.reset();
                } else {
                    NotificationManager.post("Click Pearl Pearl must be in hotbar for Legit mode!");

                }
            } else {
                usePearlSilent(slot);
                delay.reset();

            }
        }
    }

    private void usePearlSilent(int slot) {
        int prevSlot = mc.player.getInventory().selectedSlot;
        if (slot < 9) {
            mc.player.getInventory().selectedSlot = slot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = prevSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, prevSlot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, prevSlot, net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
        }
        if (swapRender.getValue()) {
            NotificationManager.post("Click Pearl: Pearl used");

        }
    }

    private int findPearl() {
        PlayerInventory inv = mc.player.getInventory();

        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).getItem() == Items.ENDER_PEARL) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).getItem() == Items.ENDER_PEARL) return i;
        }
        return -1;

    }
}