package fun.ogi.module.impl.list.misc;

import java.util.ArrayList;
import java.util.List;


import com.google.common.eventbus.Subscribe;
import fun.ogi.events.player.ClientPlayerTickEvent;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.util.inventory.ItemSlot;
import fun.ogi.util.inventory.group.SlotGroup;
import fun.ogi.util.inventory.group.SlotGroups;
import fun.ogi.util.time.Timer;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInformation(moduleName = "Inventory Cleaner", moduleDesc = "Cleans inventory from trash",moduleCategory = ModuleCategory.MISC)
public class InventoryCleaner extends Module {
   private final Timer timer = new Timer();
   private final List<Item> items = List.of(Items.STONE, Items.COBBLESTONE, Items.GRANITE, Items.IRON_ORE, Items.GOLD_ORE, Items.LAPIS_ORE);
   private final List<ItemSlot> slots = new ArrayList<>();
   public InventoryCleaner(){
   }
   @Subscribe
   private void onUpdate(EventUpdate event){
       if(this.isEnabled() && mc.player!=null &&mc.player.currentScreenHandler !=null){
           if (this.timer.finished(150L)) {
               this.slots.clear();
               SlotGroup<ItemSlot> slotsToSearch = SlotGroups.inventory().and(SlotGroups.hotbar());

               for (Item item : this.items) {
                   ItemSlot itemSlot = slotsToSearch.findItem(item);
                   if (itemSlot != null) {
                       this.slots.add(itemSlot);
                   }
               }

               if (this.slots.isEmpty()) {
                   return;
               }

               ItemSlot slot = this.slots.removeFirst();
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.getIdForServer(), 1, SlotActionType.THROW, mc.player);
               this.timer.reset();
           }
       }
   }

}

