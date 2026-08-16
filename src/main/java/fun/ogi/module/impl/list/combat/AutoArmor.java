package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Map;

@ModuleInformation(moduleName = "AutoArmor", moduleDesc = "Automatically equips best armor", moduleCategory = ModuleCategory.COMBAT)
public class AutoArmor extends Module {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final int[] ARMOR_CONTAINER_SLOTS = {5, 6, 7, 8};

    private static final Map<Item, EquipmentSlot> ITEM_SLOT_MAP = Map.ofEntries(
            Map.entry(Items.LEATHER_HELMET, EquipmentSlot.HEAD),
            Map.entry(Items.LEATHER_CHESTPLATE, EquipmentSlot.CHEST),
            Map.entry(Items.LEATHER_LEGGINGS, EquipmentSlot.LEGS),
            Map.entry(Items.LEATHER_BOOTS, EquipmentSlot.FEET),
            Map.entry(Items.CHAINMAIL_HELMET, EquipmentSlot.HEAD),
            Map.entry(Items.CHAINMAIL_CHESTPLATE, EquipmentSlot.CHEST),
            Map.entry(Items.CHAINMAIL_LEGGINGS, EquipmentSlot.LEGS),
            Map.entry(Items.CHAINMAIL_BOOTS, EquipmentSlot.FEET),
            Map.entry(Items.IRON_HELMET, EquipmentSlot.HEAD),
            Map.entry(Items.IRON_CHESTPLATE, EquipmentSlot.CHEST),
            Map.entry(Items.IRON_LEGGINGS, EquipmentSlot.LEGS),
            Map.entry(Items.IRON_BOOTS, EquipmentSlot.FEET),
            Map.entry(Items.GOLDEN_HELMET, EquipmentSlot.HEAD),
            Map.entry(Items.GOLDEN_CHESTPLATE, EquipmentSlot.CHEST),
            Map.entry(Items.GOLDEN_LEGGINGS, EquipmentSlot.LEGS),
            Map.entry(Items.GOLDEN_BOOTS, EquipmentSlot.FEET),
            Map.entry(Items.DIAMOND_HELMET, EquipmentSlot.HEAD),
            Map.entry(Items.DIAMOND_CHESTPLATE, EquipmentSlot.CHEST),
            Map.entry(Items.DIAMOND_LEGGINGS, EquipmentSlot.LEGS),
            Map.entry(Items.DIAMOND_BOOTS, EquipmentSlot.FEET),
            Map.entry(Items.NETHERITE_HELMET, EquipmentSlot.HEAD),
            Map.entry(Items.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST),
            Map.entry(Items.NETHERITE_LEGGINGS, EquipmentSlot.LEGS),
            Map.entry(Items.NETHERITE_BOOTS, EquipmentSlot.FEET),
            Map.entry(Items.TURTLE_HELMET, EquipmentSlot.HEAD)
    );

    public AutoArmor() {
    }

    @Subscribe
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        for (int i = 0; i < 4; i++) {
            int armorSlot = 3 - i;
            EquipmentSlot equipSlot = ARMOR_SLOTS[armorSlot];
            ItemStack current = mc.player.getInventory().getArmorStack(armorSlot);

            if (isTrash(current)) {
                int bestSlot = findBestArmor(equipSlot);
                if (bestSlot != -1) {
                    int sourceSlot = bestSlot;
                    if (sourceSlot >= 0 && sourceSlot <= 8) sourceSlot += 36;
                    mc.interactionManager.clickSlot(
                            mc.player.playerScreenHandler.syncId,
                            sourceSlot,
                            ARMOR_CONTAINER_SLOTS[i],
                            SlotActionType.SWAP,
                            mc.player
                    );
                }
                continue;
            }

            int betterSlot = findBetterArmor(equipSlot, current);
            if (betterSlot != -1) {
                int sourceSlot = betterSlot;
                if (sourceSlot >= 0 && sourceSlot <= 8) sourceSlot += 36;
                mc.interactionManager.clickSlot(
                        mc.player.playerScreenHandler.syncId,
                        sourceSlot,
                        ARMOR_CONTAINER_SLOTS[i],
                        SlotActionType.SWAP,
                        mc.player
                );
            }
        }
    }

    private EquipmentSlot getSlotForItem(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem)) return null;
        return ITEM_SLOT_MAP.get(stack.getItem());
    }

    private int findBestArmor(EquipmentSlot targetSlot) {
        int bestSlot = -1;
        float bestValue = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (getSlotForItem(stack) == targetSlot) {
                float value = getArmorValue(stack);
                if (value > bestValue) {
                    bestValue = value;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    private int findBetterArmor(EquipmentSlot targetSlot, ItemStack current) {
        float currentValue = getArmorValue(current);
        int bestSlot = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (getSlotForItem(stack) == targetSlot) {
                float value = getArmorValue(stack);
                if (value > currentValue) {
                    currentValue = value;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    private float getArmorValue(ItemStack stack) {
        float value = 0;
        Item item = stack.getItem();
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_BOOTS) value = 1;
        else if (item == Items.LEATHER_LEGGINGS) value = 2;
        else if (item == Items.LEATHER_CHESTPLATE) value = 3;
        else if (item == Items.CHAINMAIL_BOOTS || item == Items.GOLDEN_BOOTS) value = 1;
        else if (item == Items.GOLDEN_HELMET || item == Items.CHAINMAIL_HELMET) value = 2;
        else if (item == Items.CHAINMAIL_LEGGINGS || item == Items.GOLDEN_LEGGINGS) value = 3;
        else if (item == Items.CHAINMAIL_CHESTPLATE || item == Items.GOLDEN_CHESTPLATE) value = 4;
        else if (item == Items.IRON_HELMET || item == Items.IRON_BOOTS) value = 2;
        else if (item == Items.IRON_LEGGINGS) value = 5;
        else if (item == Items.IRON_CHESTPLATE) value = 6;
        else if (item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET || item == Items.TURTLE_HELMET) value = 3;
        else if (item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS) value = 3;
        else if (item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS) value = 6;
        else if (item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE) value = 8;
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            value += ench.getSize() * 1.5f;
        }
        return value;
    }

    private boolean isTrash(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.getItem() == Items.ELYTRA) return true;
        return (double) stack.getDamage() / stack.getMaxDamage() > 0.95;
    }
}

