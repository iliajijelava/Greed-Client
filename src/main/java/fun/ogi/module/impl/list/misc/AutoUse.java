package fun.ogi.module.impl.list.misc;

import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.impl.list.movement.Sprint;
import fun.ogi.module.settings.BooleanSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;

@ModuleInformation(moduleName = "Auto Use (Dev)", moduleDesc = "Uses something automatically", moduleCategory = ModuleCategory.MISC)
public class AutoUse extends Module {

    private final BooleanSetting invis = new BooleanSetting("Use Invisible", this, true);
    private final BooleanSetting speed = new BooleanSetting("Use Speed?", this, true);
    private final BooleanSetting food = new BooleanSetting("Use Food?", this, true);

    private int cooldownTicks = 0;
    private int prevSlot = -1;

    public AutoUse() {
        addSettings(invis, speed, food);
    }

    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }
        if (prevSlot != -1) {
            mc.player.getInventory().selectedSlot = prevSlot;
            prevSlot = -1;
        }

        if (invis.getValue() && !mc.player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            if (tryUsePotionWithEffect(StatusEffects.INVISIBILITY)) return;
        }

        if (speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            if (tryUsePotionWithEffect(StatusEffects.SPEED)) return;
        }

        if (food.getValue() && needsFood()) {
            tryEatFood();
        }
    }

    private boolean needsFood() {
        return mc.player.getHungerManager().getFoodLevel() < 20;
    }

    private int findPotionSlotByEffect(RegistryEntry<StatusEffect> effect) {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            if (stack.getItem() != Items.POTION && stack.getItem() != Items.SPLASH_POTION) continue;

            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) continue;

            if (potionHasEffect(contents, effect)) {
                return i;
            }
        }
        return -1;
    }

    private boolean potionHasEffect(PotionContentsComponent contents, RegistryEntry<StatusEffect> target) {
        for (StatusEffectInstance instance : contents.getEffects()) {
            if (instance.getEffectType().equals(target)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryUsePotionWithEffect(RegistryEntry<StatusEffect> effect) {
        int slot = findPotionSlotByEffect(effect);
        if (slot == -1) return false;

        swapAndUse(slot);
        return true;
    }

    private void tryEatFood() {
        int slot = findAnyFoodSlot();
        if (slot == -1) return;

        swapAndUse(slot);
    }

    private int findAnyFoodSlot() {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;

            FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
            if (foodComponent == null) continue;

            return i;
        }
        return -1;
    }

    private void swapAndUse(int inventorySlot) {
        int currentSelected = mc.player.getInventory().selectedSlot;

        if (inventorySlot >= 9 && inventorySlot < 36) {
            mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    inventorySlot,
                    8,
                    net.minecraft.screen.slot.SlotActionType.SWAP,
                    mc.player
            );
            mc.player.getInventory().selectedSlot = 8;
            prevSlot = currentSelected;
        } else if (inventorySlot >= 36 && inventorySlot < 45) {
            int hotbarSlot = inventorySlot - 36;
            mc.player.getInventory().selectedSlot = hotbarSlot;
            prevSlot = currentSelected;
        } else {
            return;
        }

        disableSprint();
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        cooldownTicks = 4;
        restoreSprint();
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


    private void disableSprint() {
        Sprint.pushPause(1000);
    }

    private void restoreSprint() {
        Sprint.popPause();
    }
}