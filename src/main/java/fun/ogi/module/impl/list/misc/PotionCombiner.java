package fun.ogi.module.impl.list.misc;

import baritone.api.event.events.TickEvent;
import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

@ModuleInformation(moduleName = "Potion Combiner",moduleDesc = "Automatically combines potions in anvil",moduleCategory = ModuleCategory.MISC)
public class PotionCombiner extends PveModule{
    private final ModeSetting combinerPotions = new ModeSetting("Combiner potions",this, "Strength", "Strength", "Speed", "Strength+Speed");
    private final BooleanSetting combinerAutoOpen = new BooleanSetting("Auto open",this, true);
    private final BooleanSetting combinerAutoExp = new BooleanSetting("Auto exp", this,false);
    private final SliderSetting combinerRefillTo = new SliderSetting("Refill to", this,40.0, 5.0, 100.0, 1.0);
    private BlockPos currentTarget;
    public PotionCombiner(){
        addSettings(combinerPotions,combinerAutoOpen,combinerAutoExp,combinerRefillTo);
    }
    @Subscribe
    private void onTick(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        this.handlePotionCombiner();
    }

    @Override
    public void onDisable() {
        this.currentTarget = null;
        super.onDisable();
    }
    private void handlePotionCombiner(){
        if(this.combinerAutoOpen.getValue() && mc.currentScreen==null){
            if (!this.hasPotionToCombine() && (!this.combinerAutoExp.getValue() || this.countItem(Items.EXPERIENCE_BOTTLE) >= this.combinerRefillTo.getValue())) {
                this.currentTarget = null;
                return;
            }
            ChestBlockEntity chest = this.findBlockEntity(ChestBlockEntity.class, 5.0);
            if (chest != null && this.actionTimer.finished(350L)) {
                this.currentTarget = chest.getPos();
                this.openContainer(chest.getPos(), 350L);
            }
            return;
        }
        if (!(mc.currentScreen instanceof GenericContainerScreen) || !(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) return;
        if (!this.actionTimer.finished(150L)) return;

        if (this.combinerPotions.is("Strength+Speed")) {
            if (!this.moveFirstPlayerPotion(handler, Potions.STRENGTH.value())) {
                this.moveFirstPlayerPotion(handler, Potions.SWIFTNESS.value());
            }
        } else {
            this.moveFirstPlayerPotion(handler, this.selectedCombinerPotion());
        }

        if (this.combinerAutoExp.getValue() && this.countItem(Items.EXPERIENCE_BOTTLE) < this.combinerRefillTo.getValue()) {
            this.moveFirstContainerItem(handler, stack -> stack.getItem() == Items.EXPERIENCE_BOTTLE);
        }

        this.actionTimer.reset();
    }
    private boolean hasPotionToCombine() {
        Potion first = this.selectedCombinerPotion();
        Potion second = Potions.SWIFTNESS.value();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (this.isPotionStack(stack) && (this.matchesPotion(stack, first)
                    || this.combinerPotions.is("Strength+Speed") && this.matchesPotion(stack, second))) return true;
        }
        return false;
    }

    private Potion selectedCombinerPotion() {
        return combinerPotions.is("Speed") ? Potions.SWIFTNESS.value() : Potions.STRENGTH.value();
    }

    private boolean moveFirstPlayerPotion(GenericContainerScreenHandler h, Potion potion) {
        for (int i = h.getRows() * 9; i < h.slots.size(); i++) {
            ItemStack stack = h.getSlot(i).getStack();
            if (this.isPotionStack(stack) && this.matchesPotion(stack, potion)) {
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                actionTimer.reset();
                return true;
            }
        }
        return false;
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

    private boolean isPotionStack(ItemStack s) {
        return s.getItem() == Items.POTION || s.getItem() == Items.SPLASH_POTION || s.getItem() == Items.LINGERING_POTION;
    }

    private boolean matchesPotion(ItemStack s, Potion p) {
        Potion stackPotion = getPotion(s);
        return stackPotion != null && stackPotion == p;
    }

    private Potion getPotion(ItemStack s) {
        PotionContentsComponent c = s.get(DataComponentTypes.POTION_CONTENTS);
        if (c == null || c.potion().isEmpty()) return null;
        return c.potion().get().value();
    }

}

