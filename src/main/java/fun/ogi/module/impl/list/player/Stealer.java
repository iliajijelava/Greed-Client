package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.time.Timer;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Random;

@ModuleInformation(moduleName = "Stealer", moduleDesc = "Steals items from chests", moduleCategory = ModuleCategory.PLAYER)
public class Stealer extends Module {

    private final SliderSetting delay = new SliderSetting("Delay", this, 0.2, 0.0, 2.0, 0.05);
    private final BooleanSetting closeOnEmpty = new BooleanSetting("Close", this, true);
    private final BooleanSetting autoDisable = new BooleanSetting("Auto Off", this, true);
    private final ModeSetting mode = new ModeSetting("Mode", this, "Up", "Up", "Down", "Center", "Random");

    private final Timer clickTimer = new Timer();
    private final Random random = new Random();

    public Stealer() {
        addSettings(delay, closeOnEmpty, autoDisable, mode);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (!(mc.currentScreen instanceof GenericContainerScreen)) return;
        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) return;

        int size = handler.getInventory().size();

        String currentMode = mode.getValueAsString();
        long delayMs = (long) (delay.getFloatValue() * 1000.0F);

        switch (currentMode) {
            case "Up" -> stealTopToBottom(handler, size, delayMs);
            case "Down" -> stealBottomToTop(handler, size, delayMs);
            case "Center" -> stealFromCenter(handler, size, delayMs);
            case "Random" -> stealRandom(handler, size, delayMs);
        }

        if (isContainerEmpty(handler.getInventory())) {
            if (autoDisable.getValue()) {
                toggle();
            }
            if (closeOnEmpty.getValue() && mc.player.currentScreenHandler != null) {
                mc.player.closeHandledScreen();
            }
        }
    }

    private void stealTopToBottom(GenericContainerScreenHandler handler, int size, long delayMs) {
        for (int i = 0; i < size; i++) {
            if (!clickTimer.finished(delayMs + random(-50, 50))) break;
            if (!handler.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                clickTimer.reset();
            }
        }
    }

    private void stealBottomToTop(GenericContainerScreenHandler handler, int size, long delayMs) {
        for (int i = size - 1; i >= 0; i--) {
            if (!clickTimer.finished(delayMs + random(-50, 50))) break;
            if (!handler.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                clickTimer.reset();
            }
        }
    }

    private void stealFromCenter(GenericContainerScreenHandler handler, int size, long delayMs) {
        int center = size / 2;
        for (int offset = 0; offset <= center; offset++) {
            if (!clickTimer.finished(delayMs + random(-50, 50))) break;
            int left = center - offset;
            int right = center + offset;
            if (left >= 0 && !handler.getSlot(left).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(handler.syncId, left, 0, SlotActionType.QUICK_MOVE, mc.player);
                clickTimer.reset();
            } else if (right < size && right != left && !handler.getSlot(right).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(handler.syncId, right, 0, SlotActionType.QUICK_MOVE, mc.player);
                clickTimer.reset();
            }
        }
    }

    private void stealRandom(GenericContainerScreenHandler handler, int size, long delayMs) {
        if (!clickTimer.finished(delayMs + random(-50, 50))) return;
        int slot = random.nextInt(size);
        if (!handler.getSlot(slot).getStack().isEmpty()) {
            mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
            clickTimer.reset();
        }
    }

    private boolean isContainerEmpty(Inventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.getStack(i).isEmpty()) return false;
        }
        return true;
    }

    private long random(long min, long max) {
        return (long) (random.nextDouble() * (max - min) + min);
    }
}

