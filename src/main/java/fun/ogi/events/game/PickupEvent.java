package fun.ogi.events.game;


import fun.ogi.events.Event;
import net.minecraft.item.ItemStack;

public class PickupEvent extends Event {
   public ItemStack itemStack;
   public int count;

   public PickupEvent(ItemStack itemStack, int count) {
      this.itemStack = itemStack;
      this.count = count;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public int getCount() {
      return this.count;
   }
}

