package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.EventKeyboard;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.KeySetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.NotificationManager;
import fun.ogi.util.player.InventoryUtils;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import fun.ogi.util.time.Timer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;

import static fun.ogi.util.MinecraftUtil.mc;

@ModuleInformation(moduleName = "Mine Helper", moduleDesc = "Helps in Mine", moduleCategory = ModuleCategory.PLAYER)
public class MineHelper extends Module {
    private final BooleanSetting save = new BooleanSetting("Safe pickaxe", this, true);
    public final SliderSetting percent = new SliderSetting("Durability (%)", this, 10, 1, 70, 1);
    private final BooleanSetting autoReplace = new BooleanSetting("Auto Replace", this, true);
    private final BooleanSetting autoRepair = new BooleanSetting("Auto fix", this, true);
    private final KeySetting bind = new KeySetting("Fix key (Works only when Auto Fix is enabled)", this, -1);
    private final Timer timer = new Timer();
    private boolean rotate;

    @Subscribe
    public void onKeyboard(EventKeyboard event) {
        if (bind.isKeyDown() && event.getAction() == 1) {
            this.rotate = true;
            this.repairPickaxeWithBottle();
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (this.rotate) {
            RotationComponent.update(
                    new Rotation(mc.player.getYaw(), 90.0F),
                    180.0F, 180.0F, 180, 0
            );
        }
    }

    public void onBlockBreak(net.minecraft.util.math.BlockPos blockPos) {
        if (mc.player == null) return;

        ItemStack currentStack = mc.player.getMainHandStack();
        if (this.isValidPickaxe(currentStack)) {
            double durabilityPercent = this.getDurabilityPercent(currentStack);
            if (this.save.getValue() && !(durabilityPercent >= this.percent.getValue())) {
                this.handleLowDurability(currentStack);
            }
        }
    }

    private void handleLowDurability(ItemStack currentStack) {
        boolean switched = false;
        if (this.autoReplace.getValue()) {
            switched = this.trySwitchPickaxe(currentStack);
        }

        if (!switched && this.timer.finished(800L)) {
            NotificationManager.post("Кирка почти сломана! Нет замены/опыта для починки", NotificationManager.TYPE_ERROR, 2500, '!');
            this.timer.reset();
        }
    }

    private void repairPickaxeWithBottle() {
        if (mc.player != null && mc.currentScreen == null) {
            ItemStack pickaxe = mc.player.getMainHandStack();
            if (!this.isValidPickaxe(pickaxe) || pickaxe.getDamage() == 0) {
                NotificationManager.post("Нет кирки или она не повреждена!", NotificationManager.TYPE_ERROR, 2500, '!');
                this.rotate = false;
            } else if (this.ensureBottleInOffhand()) {
                this.useExperienceBottle();
            }
        }
    }

    private boolean ensureBottleInOffhand() {
        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() == Items.EXPERIENCE_BOTTLE) {
            return true;
        }

        int bottleSlot = InventoryUtils.getSlot(Items.EXPERIENCE_BOTTLE);
        if (bottleSlot == -1) {
            NotificationManager.post("Нет бутылок опыта!", NotificationManager.TYPE_ERROR, 2500, '!');
            this.rotate = false;
            return false;
        }

        int containerSlot = InventoryUtils.toContainerSlot(bottleSlot);
        InventoryUtils.moveItem(containerSlot, 40);
        return true;
    }

    private void useExperienceBottle() {
        if (mc.player.getOffHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
            mc.options.useKey.setPressed(false);
            this.rotate = false;
        } else {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            mc.player.swingHand(Hand.OFF_HAND);
        }
    }

    private boolean trySwitchPickaxe(ItemStack currentStack) {
        int bestSlot = this.findBestPickaxeSlot(currentStack);
        if (bestSlot == -1) {
            return false;
        }

        mc.player.getInventory().selectedSlot = bestSlot;

        if (this.timer.finished(800L)) {
            ItemStack newStack = mc.player.getInventory().getStack(bestSlot);
            NotificationManager.post(
                    String.format("Заменил кирку с %.1f%% на %.1f%%", this.getDurabilityPercent(currentStack), this.getDurabilityPercent(newStack)),
                    NotificationManager.TYPE_SUCCESS,
                    2500,
                    'J'
            );
            this.timer.reset();
        }

        return true;
    }

    private int findBestPickaxeSlot(ItemStack currentStack) {
        double currentDurability = this.getDurabilityPercent(currentStack);
        int bestSlot = -1;
        double bestDurability = currentDurability;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (this.isValidPickaxe(stack)) {
                double durability = this.getDurabilityPercent(stack);
                if (durability > bestDurability) {
                    bestDurability = durability;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    private boolean isValidPickaxe(ItemStack stack) {
        return stack != null && stack.isDamageable() && stack.getItem() instanceof PickaxeItem;
    }

    private double getDurabilityPercent(ItemStack stack) {
        return (double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage() * 100.0;
    }
}

