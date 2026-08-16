package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@ModuleInformation(moduleName = "Spider", moduleDesc = "Allows you to climb up walls", moduleCategory = ModuleCategory.MOVEMENT)
public class Spider extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", this, "Water", "Water", "SpookyTime");
    private final BooleanSetting legit = new BooleanSetting("Legit", this, false);

    private int lastSlot = -1;
    private boolean isClimbing = false;
    private int swapBackSlot = -1;
    private int spookyTicks;
    private int chargeSlot = -1;
    private boolean charging;

    public Spider() {
        addSettings(mode, legit);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player == null) return;

        if (lastSlot != -1 && legit.getValue()) {
            mc.player.getInventory().selectedSlot = lastSlot;
        }

        lastSlot = -1;
        swapBackSlot = -1;
        isClimbing = false;
        spookyTicks = 0;
        chargeSlot = -1;
        charging = false;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (!mc.player.horizontalCollision) {
            stopClimbing();
            return;
        }

        isClimbing = true;
        mc.player.setPitch(0);

        if (mode.getValue().equals("SpookyTime")) {
            processSpookyTime();
            return;
        }

        int bucketSlot = getBucketSlot(false);
        if (bucketSlot == -1) return;

        useBucket(bucketSlot, legit.getValue());
        mc.player.setVelocity(mc.player.getVelocity().x, 0.36, mc.player.getVelocity().z);
    }

    private void stopClimbing() {
        if (lastSlot != -1 && legit.getValue()) {
            mc.player.getInventory().selectedSlot = lastSlot;
            lastSlot = -1;
        }

        if (swapBackSlot != -1) {
            mc.interactionManager.clickSlot(0, swapBackSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
            swapBackSlot = -1;
        }

        isClimbing = false;
        spookyTicks = 0;
        chargeSlot = -1;
        charging = false;
    }

    private void processSpookyTime() {
        int bucketSlot = getBucketSlot(true);
        boolean bucketPulse = spookyTicks % 5 == 0;
        boolean boostPulse = spookyTicks % 4 != 3;

        keepChargeHeld();

        if (bucketSlot != -1 && bucketPulse) {
            useBucket(bucketSlot, false);
            keepChargeHeld();
        }

        double y = boostPulse ? 0.18 : 0.03;
        mc.player.setVelocity(mc.player.getVelocity().x, y, mc.player.getVelocity().z);
        spookyTicks++;
    }

    private void useBucket(int bucketSlot, boolean legitMode) {
        if (!legitMode) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            boolean isInventorySwap = bucketSlot >= 9 && bucketSlot <= 35;

            if (isInventorySwap) {
                mc.interactionManager.clickSlot(0, bucketSlot, currentSlot, SlotActionType.SWAP, mc.player);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.interactionManager.clickSlot(0, bucketSlot, currentSlot, SlotActionType.SWAP, mc.player);
            } else {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(bucketSlot));
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
            }
            return;
        }

        boolean isInventorySwap = bucketSlot >= 9 && bucketSlot <= 35;

        if (isInventorySwap) {
            mc.interactionManager.clickSlot(0, bucketSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            swapBackSlot = bucketSlot;
        } else if (mc.player.getInventory().selectedSlot != bucketSlot) {
            if (lastSlot == -1) {
                lastSlot = mc.player.getInventory().selectedSlot;
            }
            mc.player.getInventory().selectedSlot = bucketSlot;
        }

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
    }

    private void keepChargeHeld() {
        if (isChargeItem(mc.player.getOffHandStack())) {
            if (!charging || spookyTicks % 12 == 0) {
                sendChargeUsePacket(Hand.OFF_HAND);
            }
            charging = true;
            return;
        }

        if (chargeSlot == -1 || !isChargeItem(mc.player.getInventory().getStack(chargeSlot))) {
            chargeSlot = getChargeHotbarSlot();
            charging = false;
        }
        if (chargeSlot == -1) return;

        if (mc.player.getInventory().selectedSlot != chargeSlot) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(chargeSlot));
            mc.player.getInventory().selectedSlot = chargeSlot;
            charging = false;
        }

        if (!charging || spookyTicks % 12 == 0) {
            sendChargeUsePacket(Hand.MAIN_HAND);
        }
        charging = true;
    }

    private void sendChargeUsePacket(Hand hand) {
        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(hand, 0, mc.player.getYaw(), mc.player.getPitch()));
    }

    private int getBucketSlot(boolean allowLava) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isBucket(stack, allowLava)) {
                return i;
            }
        }

        if (!legit.getValue() || mode.getValue().equals("SpookyTime")) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (isBucket(stack, allowLava)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private int getChargeHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (isChargeItem(mc.player.getInventory().getStack(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isBucket(ItemStack stack, boolean allowLava) {
        return stack.getItem() == Items.WATER_BUCKET || allowLava && stack.getItem() == Items.LAVA_BUCKET;
    }

    private boolean isChargeItem(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof TridentItem;
    }
}

