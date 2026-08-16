package fun.ogi.util.inventory.group;


import fun.ogi.util.inventory.group.impl.ArmorSlotsGroup;
import fun.ogi.util.inventory.group.impl.HotbarSlotsGroup;
import fun.ogi.util.inventory.group.impl.InventorySlotsGroup;
import fun.ogi.util.inventory.group.impl.OffhandSlotGroup;
import fun.ogi.util.inventory.slots.ArmorSlot;
import fun.ogi.util.inventory.slots.HotbarSlot;
import fun.ogi.util.inventory.slots.InventorySlot;
import fun.ogi.util.inventory.slots.OffhandSlot;

public class SlotGroups {
   private SlotGroups() {
   }

   public static SlotGroup<HotbarSlot> hotbar() {
      return new HotbarSlotsGroup();
   }

   public static SlotGroup<InventorySlot> inventory() {
      return new InventorySlotsGroup();
   }

   public static SlotGroup<ArmorSlot> armor() {
      return new ArmorSlotsGroup();
   }

   public static SlotGroup<OffhandSlot> offhand() {
      return new OffhandSlotGroup();
   }
}

