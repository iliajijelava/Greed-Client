package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.EventMouse;
import fun.ogi.mixin.HandledScreenAccessor;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInformation(moduleName = "Item Scroller", moduleDesc = "Quick move items with shift+click", moduleCategory = ModuleCategory.PLAYER)
public class ItemScroller extends Module {

    public static ItemScroller INSTANCE = new ItemScroller();

    public final SliderSetting delay = new SliderSetting("Delay", this, 0.0f, 0.0f, 200.0f, 1.0f);

    public ItemScroller() {
        addSettings(delay);
    }

    @Subscribe
    public void onMouse(EventMouse event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.interactionManager == null) return;
        if (mc.currentScreen == null) return;
        if (!(mc.currentScreen instanceof GenericContainerScreen)) return;
        if (mc.player.currentScreenHandler == null) return;

        boolean shift = mc.options.sneakKey.isPressed();
        if (!shift) return;

        if (event.getButton() != 0) return;
        if (event.getAction() != 1) return;

        HandledScreen<?> screen = (HandledScreen<?>) mc.currentScreen;
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        Slot focusedSlot = accessor.getItemScroller$getFocusedSlot();
        if (focusedSlot == null) return;

        ItemStack clickedStack = focusedSlot.getStack();
        if (clickedStack.isEmpty()) return;

        int syncId = mc.player.currentScreenHandler.syncId;

        for (int i = 0; i < mc.player.currentScreenHandler.slots.size(); i++) {
            Slot slot = mc.player.currentScreenHandler.getSlot(i);
            if (slot == focusedSlot) continue;
            if (slot.getStack().isEmpty()) continue;
            if (!slot.getStack().getItem().equals(clickedStack.getItem())) continue;

            mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }
}

