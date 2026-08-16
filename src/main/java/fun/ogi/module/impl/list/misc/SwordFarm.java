package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.module.settings.StringSetting;
import fun.ogi.util.NotificationManager;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.time.Timer;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

@ModuleInformation(
   moduleName = "Sword Farm",
   moduleDesc = "Automatically takes swords from container and sells on auction",
   moduleCategory = ModuleCategory.MISC
)
public class SwordFarm extends Module {
   private final StringSetting swordPrice = new StringSetting("Sword Price", this, "15000");
   private final SliderSetting swordRelistCooldown = new SliderSetting("Relist cooldown (sec)", this, 60.0, 5.0, 300.0, 5.0);
   private final BooleanSetting swordTakeFromContainer = new BooleanSetting("Take from open container", this, false);

   private final Timer sellTimer = new Timer();
   private final Timer actionTimer = new Timer();

   public SwordFarm() {
      addSettings(swordPrice, swordRelistCooldown, swordTakeFromContainer);
      sellTimer.reset();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
      this.handleSwordFarm();
   }

   private void handleSwordFarm() {
      try {
         this.tickSwordFarm();
      } catch (Exception e) {
         this.warn("Sword Farm error: " + e.getMessage());
      }
   }

   private void tickSwordFarm() {
      if (!this.ensureHotbarItem(stack -> stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem, 80.0)) {
         if (this.swordTakeFromContainer.getValue()
            && mc.currentScreen instanceof GenericContainerScreen
            && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
            if (!this.moveFirstContainerItem(handler, stack -> stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem)) {
               this.warn("Sword Farm", "No sword found in container!");
            }
            return;
         }
         this.warn("Sword Farm", "No sword found in hotbar!");
         return;
      }

      if (mc.getNetworkHandler() != null && this.sellTimer.finished((long) (swordRelistCooldown.getValue() * 1000.0))) {
         String price = this.swordPrice.getText().strip();
         if (price.isEmpty() || price.chars().allMatch(ch -> ch == '0')) {
            this.warn("Sword Farm", "Set a valid sell price!");
            return;
         }
         mc.getNetworkHandler().sendChatCommand("ah sell " + price);
         this.sellTimer.reset();
      }
      Timer rellistTimer = new Timer();
      if(mc.getNetworkHandler() !=null && rellistTimer.finished(65000L)){
          ChatUtil.sendMSG("TURN ON AUTO RESELL");
      }

   }

   private boolean moveFirstContainerItem(GenericContainerScreenHandler h, Predicate<ItemStack> p) {
      for (int i = 0; i < h.getRows() * 9; i++) {
         if (p.test(h.getSlot(i).getStack())) {
            mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            actionTimer.reset();
            return true;
         }
      }
      return false;
   }

   private boolean ensureHotbarItem(Predicate<ItemStack> predicate, double delay) {
      if (predicate.test(mc.player.getMainHandStack())) return true;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (predicate.test(stack)) {
            mc.player.getInventory().selectedSlot = i;
            return true;
         }
      }
      return false;
   }

   private void warn(String title, String text) {
      NotificationManager.post(title + ": " + text);
   }

   private void warn(String text) {
      NotificationManager.post(text);
   }
}

