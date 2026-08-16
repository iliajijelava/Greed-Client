package fun.ogi.util.inventory.group.impl;

import fun.ogi.util.inventory.group.SlotGroup;
import fun.ogi.util.inventory.slots.InventorySlot;

import java.util.ArrayList;
import java.util.List;


public class InventorySlotsGroup extends SlotGroup<InventorySlot> {
   public InventorySlotsGroup() {
      super(createSlots());
   }

   private static List<InventorySlot> createSlots() {
      List<InventorySlot> slots = new ArrayList<>();

      for (int i = 0; i < 27; i++) {
         slots.add(new InventorySlot(i));
      }

      return slots;
   }
}

