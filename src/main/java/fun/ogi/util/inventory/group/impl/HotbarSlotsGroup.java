package fun.ogi.util.inventory.group.impl;

import fun.ogi.util.inventory.group.SlotGroup;
import fun.ogi.util.inventory.slots.HotbarSlot;

import java.util.ArrayList;
import java.util.List;


public class HotbarSlotsGroup extends SlotGroup<HotbarSlot> {
   public HotbarSlotsGroup() {
      super(createSlots());
   }

   private static List<HotbarSlot> createSlots() {
      List<HotbarSlot> slots = new ArrayList<>();

      for (int i = 0; i < 9; i++) {
         slots.add(new HotbarSlot(i));
      }

      return slots;
   }
}

