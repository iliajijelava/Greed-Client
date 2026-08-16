package fun.ogi.util.inventory.slots;


import fun.ogi.util.inventory.ItemSlot;
import net.minecraft.item.ItemStack;

import static fun.ogi.util.MinecraftUtil.mc;

public class InventorySlot extends ItemSlot {
   private final int slotId;

   public InventorySlot(int slotId) {
      if (slotId >= 0 && slotId <= 26) {
         this.slotId = slotId;
      } else {
         throw new IllegalArgumentException("Inventory Slot ID must be between 0 and 26");
      }
   }

   @Override
   public ItemStack itemStack() {
      return mc.player != null && mc.player.getInventory() != null ? mc.player.getInventory().getStack(this.slotId + 9) : ItemStack.EMPTY;
   }

   @Override
   public int getIdForServer() {
      return this.slotId + 9;
   }

   public int getSlotId() {
      return this.slotId;
   }
}

