package fun.ogi.module.impl.list.misc;


import com.google.common.eventbus.Subscribe;
import fun.ogi.events.player.ClientPlayerTickEvent;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;

@ModuleInformation(moduleName = "Fast Item Use",moduleDesc = "Fast use items",moduleCategory = ModuleCategory.MISC)
public class FastItemUse extends Module {
   private final BooleanSetting bow = new BooleanSetting("Use bow ",this,true);
   private final BooleanSetting trident = new BooleanSetting("Use trident",this,true);
   private final BooleanSetting crossbow = new BooleanSetting("Use crossbow",this,true);
   public FastItemUse(){
       addSettings(bow,trident,crossbow);
   }
   @Subscribe
   private void onClientPLayerTickEvent(ClientPlayerTickEvent event){
       if (this.trident.getValue() && this.canReleaseTrident()) {
           this.releaseItem();
       }

       if (this.bow.getValue() && this.canReleaseBow()) {
           this.releaseItem();
       }

       if (this.crossbow.getValue() && this.canReleaseCrossbow()) {
           this.releaseItem();
       }
   }

   private void releaseItem() {
      if (mc.player != null) {
         mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, mc.player.getHorizontalFacing()));
         mc.player.stopUsingItem();
      }
   }

   private boolean canReleaseTrident() {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack heldStack = mc.player.getMainHandStack();
         return heldStack.getItem() == Items.TRIDENT

            && mc.player.isUsingItem()
            && mc.player.getItemUseTime() >= 10.0
            && mc.player.getAttackCooldownProgress(0.5F) > 0.92F;
      }
   }

   private boolean canReleaseBow() {
      return mc.player == null ? false : mc.player.getMainHandStack().getItem() == Items.BOW && mc.player.isUsingItem() && mc.player.getItemUseTime() >= 10.0;
   }

   private boolean canReleaseCrossbow() {
      return mc.player == null
         ? false
         : mc.player.getMainHandStack().getItem() == Items.CROSSBOW && mc.player.isUsingItem() && mc.player.getItemUseTime() >= 10;
   }
}

