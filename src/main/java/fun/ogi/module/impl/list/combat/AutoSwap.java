package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.player.EventMoveInput;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.impl.list.movement.Sprint;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.KeySetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.util.NotificationManager;
import fun.ogi.util.chatutil.ChatUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInformation(moduleName = "Auto Swap", moduleDesc = "Swapaet blad.", moduleCategory = ModuleCategory.COMBAT)
public class AutoSwap extends Module {
    private ModeSetting firstItem = new ModeSetting("First Item", this, "Totem", "Totem", "Shar","Shield","Gapple");
    private  ModeSetting secondItem = new ModeSetting("Second Item", this, "Totem", "Totem ", "Shar","Shield","Gapple");
    private KeySetting keyBind = new KeySetting("Keybind", this,-1);
    private final BooleanSetting swapRender = new BooleanSetting("Show swap", this, true);
    private final BooleanSetting onlyEnchanted = new BooleanSetting("Only enchanted totems", this, false);
    private final BooleanSetting bypassgrim = new BooleanSetting("Servers bypass", this,true);
    private boolean isFirstItem = true;
    private boolean triggerSwap;
    private long swapTime;
    private long swapKeyTime;
    private boolean bypassActive;
    private boolean bypassSwapped;
    private int bypassSlot = -1;
    private String bypassItemName = "";
    private int bypassTicks;
    private boolean sprintPaused;
    private int swapCooldown;
    private int targetSlot = -1;
    private boolean needSwap = false;

    public AutoSwap(){
        addSetting(firstItem);
        addSetting(secondItem);
        addSetting(keyBind);
        addSetting(swapRender);
        addSetting(onlyEnchanted);
    }
    @Subscribe
    public void onUpdate(final EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (swapCooldown > 0) {
            swapCooldown--;
        }

        if (bypassgrim.getValue() && bypassTicks > 0) {
            mc.player.setSprinting(false);
            bypassTicks--;

            if (bypassTicks == 1) {
                performSwap();
            }

            if (bypassTicks == 0) {
                restoreSprint();
            }
            return;
        }

        if (needSwap && targetSlot == -1) {
            needSwap = false;

            Item offhand = mc.player.getOffHandStack().getItem();
            Item first = getItem(firstItem.getValue());
            Item second = getItem(secondItem.getValue());

            int firstSlot = findItemSlot(first);
            int secondSlot = findItemSlot(second);

            if (firstSlot == -1 && secondSlot == -1) return;

            int slot;
            if (offhand == first && secondSlot != -1) {
                slot = secondSlot;
            } else if (firstSlot != -1) {
                slot = firstSlot;
            } else {
                slot = secondSlot;
            }

            if (slot == -1) return;

            targetSlot = slot;

            if (bypassgrim.getValue()) {
                disableSprint();
                bypassTicks = 2;
                swapCooldown = 2;
            } else {
                performSwap();
                swapCooldown = 2;
            }
        }
    }



    @Subscribe
    public void onInput(final EventMoveInput e) {
        if (bypassgrim.getValue() && bypassTicks > 0) {
            if (mc.player == null) return;
            mc.player.setSprinting(false);
            e.setForward(0);
            e.setStrafe(0);
            e.setJump(false);
            e.setSneak(false);
        }
    }

    private void performSwap() {
        if (targetSlot == -1) return;

        doSwap(targetSlot);
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));

        targetSlot = -1;
    }

    private void doSwap(int slot) {
        if (slot >= 36 && slot <= 44) {
            int hotbarSlot = slot - 36;
            mc.interactionManager.clickSlot(0, 45, hotbarSlot, SlotActionType.SWAP, mc.player);
        } else {
            mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
        }
    }


    private int findItemSlot(Item item) {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }


    private Item getItem(String name) {
        return switch (name) {

            case "Totem" -> Items.TOTEM_OF_UNDYING;
            case "Shar" -> Items.PLAYER_HEAD;
            case "Gapple" -> Items.GOLDEN_APPLE;
            case "Shield" -> Items.SHIELD;
            default -> Items.AIR;
        };
    }

    private void disableSprint() {
        if (sprintPaused) {
            return;
        }

        Sprint.pushPause(1000);
        sprintPaused = true;
    }

    private void restoreSprint() {
        if (!sprintPaused) {
            return;
        }

        sprintPaused = false;
        Sprint.popPause();
    }


    @Override
    public void onDisable() {
        super.onDisable();
        bypassActive = false;
        bypassSwapped = false;
        bypassSlot = -1;
        isFirstItem = true;
    }
}

