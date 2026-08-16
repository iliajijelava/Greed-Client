package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.impl.list.movement.InventoryMove;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.KeySetting;
import fun.ogi.util.NotificationManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

@ModuleInformation(moduleName = "Elytra Swap" , moduleDesc = "Blad ti seryezno ne ponimaesh shto eta zalupa delaet?", moduleCategory = ModuleCategory.MISC)
public class ElytraSwap extends Module {

    public static ElytraSwap INSTANCE = new ElytraSwap();

    private static final long SWAP_COOLDOWN_MS = 600L;
    private static final long FIREWORK_COOLDOWN_MS = 50L;
    private static final long PENDING_FIREWORK_TIMEOUT_MS = 500L;

    private final KeySetting elytraBind = new KeySetting("Elytra Bind:", this, -1);
    private final KeySetting fireworkBind = new KeySetting("Firework Bind", this,-1);
    private final BooleanSetting autoFly = new BooleanSetting("Auto Fly", this,true);
    private final BooleanSetting syncGuiMove = new BooleanSetting("Sync with Inventory Move", this,true);

    private ItemStack currentChest = ItemStack.EMPTY;

    private long swapLastMs = 0L;
    private long fireworkLastMs = 0L;
    private long pendingFireworkUntil = 0L;

    private boolean swapQueued = false;
    private boolean fireworkQueued = false;
    public ElytraSwap(){
        addSettings(elytraBind,fireworkBind,autoFly,syncGuiMove);
    }
    @Subscribe
    public void onUpdate(final EventUpdate ignored) {
        if (mc.player == null || mc.world == null) return;

        currentChest = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        if (swapQueued) {
            swapQueued = false;
            trySwap();
        }

        if (fireworkQueued) {
            fireworkQueued = false;
            requestFirework();
        }

        if (autoFly.getValue() && currentChest.isOf(Items.ELYTRA)) {
            tryTakeoff(currentChest);
        }
        if (elytraBind.isPressed()) swapQueued = true;
        if (fireworkBind.isPressed()) fireworkQueued = true;
        tryLaunchPendingFirework();
    }



    private void trySwap() {
        if (System.currentTimeMillis() - swapLastMs < SWAP_COOLDOWN_MS) return;
        doChangeChest(currentChest);
        swapLastMs = System.currentTimeMillis();
    }

    private void doChangeChest(ItemStack chest) {
        if (chest.isOf(Items.ELYTRA)) {
            int armorSlot = findChestplateSlot();
            if (armorSlot < 0) {
                NotificationManager.post(Formatting.RED + "" + Formatting.BOLD + "Chestplate didn't found!");
                return;
            }
            moveToChestSlot(armorSlot);
        } else {
            int elytraSlot = findItemSlot(Items.ELYTRA);
            if (elytraSlot < 0) {
                NotificationManager.post(Formatting.RED + "" + Formatting.BOLD + "Elytra didn't found!");
                return;
            }
            moveToChestSlot(elytraSlot);
        }
    }

    private void moveToChestSlot(int slot) {
        InventoryMove guiMove = InventoryMove.INSTANCE;
        boolean guiActive = guiMove != null && guiMove.isEnabled() && syncGuiMove.getValue();

        if (guiActive) guiMove.swapBypass = true;

        if (slot >= 0 && slot < 9) {
            mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, mc.player);
        } else {
            mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, 6, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
        }
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));

        if (guiActive) guiMove.swapBypass = false;
    }

    private void requestFirework() {
        if (System.currentTimeMillis() - fireworkLastMs < FIREWORK_COOLDOWN_MS) return;
        if (!hasFirework()) return;

        if (launchFirework()) {
            fireworkLastMs = System.currentTimeMillis();
            pendingFireworkUntil = 0L;
            return;
        }

        if (canWaitForFlight()) {
            pendingFireworkUntil = System.currentTimeMillis() + PENDING_FIREWORK_TIMEOUT_MS;
        }
    }

    private void tryLaunchPendingFirework() {
        if (pendingFireworkUntil == 0L) return;
        if (System.currentTimeMillis() > pendingFireworkUntil || !hasFirework()) {
            pendingFireworkUntil = 0L;
            return;
        }
        if (launchFirework()) {
            fireworkLastMs = System.currentTimeMillis();
            pendingFireworkUntil = 0L;
        }
    }

    private boolean launchFirework() {
        if (!mc.player.isGliding()) return false;
        if (!hasFirework()) return false;

        InventoryMove guiMove = InventoryMove.INSTANCE;
        boolean guiActive = guiMove != null && guiMove.isEnabled() && syncGuiMove.getValue();

        if (guiActive) guiMove.swapBypass = true;

        int slot = findItemSlot(Items.FIREWORK_ROCKET);
        int oldSlot = mc.player.getInventory().selectedSlot;

        if (slot >= 36) {
            mc.player.getInventory().selectedSlot = slot - 36;
        } else {
            mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
            mc.player.getInventory().selectedSlot = 0;
        }

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

        if (slot < 36) {
            mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
            mc.player.getInventory().selectedSlot = oldSlot;
        }

        if (guiActive) guiMove.swapBypass = false;

        return true;
    }

    private boolean canWaitForFlight() {
        return currentChest.isOf(Items.ELYTRA)
                || mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private boolean hasFirework() {
        return findItemSlot(Items.FIREWORK_ROCKET) >= 0;
    }

    private void tryTakeoff(ItemStack chest) {
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return;
        if (mc.player.isOnGround()) {
            mc.player.jump();
        } else if (isElytraUsable(chest) && !mc.player.isGliding() && !mc.player.getAbilities().flying) {
            mc.player.startGliding();
            mc.player.networkHandler.sendPacket(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
        }
    }

    private boolean isElytraUsable(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(item)) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    private int findChestplateSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            net.minecraft.item.Item item = stack.getItem();
            if (item == Items.LEATHER_CHESTPLATE
                    || item == Items.CHAINMAIL_CHESTPLATE
                    || item == Items.IRON_CHESTPLATE
                    || item == Items.GOLDEN_CHESTPLATE
                    || item == Items.DIAMOND_CHESTPLATE
                    || item == Items.NETHERITE_CHESTPLATE) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        pendingFireworkUntil = 0L;
        swapQueued = false;
        fireworkQueued = false;
        super.onDisable();
    }
}