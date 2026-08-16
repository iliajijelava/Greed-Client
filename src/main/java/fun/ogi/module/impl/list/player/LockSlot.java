package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.PacketEvent;
import fun.ogi.mixin.SlotAccessor;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.mixin.SlotAccessor;
import fun.ogi.util.chatutil.ChatUtil;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInformation(moduleName = "Lock Slot",moduleDesc= "Locks chosen slots",moduleCategory = ModuleCategory.PLAYER)
public class LockSlot extends Module {
    public static LockSlot INSTANCE = new LockSlot();

    private final ListSetting slots = new ListSetting("Slots", this, "1","2","3","4","5","6","7","8","9");

    public LockSlot() {
        addSettings(slots);
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (mc.player == null || event.getType() != PacketEvent.Type.SEND) return;
        if (mc.currentScreen instanceof HandledScreen<?>) return;

        if (event.getPacket() instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() != PlayerActionC2SPacket.Action.DROP_ITEM
                    && packet.getAction() != PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
                return;
            }
            if (isCurrentSlotLockedForDrop()) {
                event.cancelEvent();
                sendLockedMessage(mc.player.getInventory().selectedSlot);
            }
            return;
        }

        if (event.getPacket() instanceof ClickSlotC2SPacket packet && packet.getActionType() == SlotActionType.THROW) {
            int hotbarSlot = getHotbarSlotFromClick(packet.getSlot());
            if (hotbarSlot >= 0 && isHotbarSlotLocked(hotbarSlot)) {
                event.cancelEvent();
                sendLockedMessage(hotbarSlot);
            }
        }
    }

    public boolean isCurrentSlotLockedForDrop() {
        if (mc.player == null || mc.player.getMainHandStack().isEmpty()) return false;
        if (mc.currentScreen instanceof HandledScreen<?>) return false;
        return isHotbarSlotLocked(mc.player.getInventory().selectedSlot);
    }

    private boolean isHotbarSlotLocked(int slot) {
        String slotName = String.valueOf(slot + 1);
        return slots.isSelected(slotName);
    }

    private int getHotbarSlotFromClick(int slotId) {
        if (mc.player == null || slotId < 0 || slotId >= mc.player.currentScreenHandler.slots.size()) {
            return -1;
        }

        Slot slot = mc.player.currentScreenHandler.getSlot(slotId);
        SlotAccessor accessor = (SlotAccessor) slot;
        int inventoryIndex = accessor.elysium$getIndex();
        if (accessor.elysium$getInventory() == mc.player.getInventory() && inventoryIndex >= 0 && inventoryIndex <= 8) {
            return inventoryIndex;
        }
        return -1;
    }

    private void sendLockedMessage(int slot) {
        ChatUtil.sendMSG("Item drop from slot " + (slot + 1) + " blocked");
    }
}

