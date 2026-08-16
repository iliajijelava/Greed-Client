package fun.ogi.util.inventory.group.impl;

import fun.ogi.util.inventory.group.SlotGroup;
import fun.ogi.util.inventory.slots.OffhandSlot;

import java.util.List;


public class OffhandSlotGroup extends SlotGroup<OffhandSlot> {
   public OffhandSlotGroup() {
      super(List.of(new OffhandSlot()));
   }
}

