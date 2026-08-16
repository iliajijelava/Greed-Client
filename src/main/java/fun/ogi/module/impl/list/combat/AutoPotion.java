package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ListSetting;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@ModuleInformation(moduleName = "AutoPotion", moduleDesc = "Automatically throws splash potions", moduleCategory = ModuleCategory.COMBAT)
public class AutoPotion extends Module {

    private final ListSetting potions = new ListSetting("Potions", this, "Strength", "Speed", "Fire Resistance");
    private final SliderSetting throwDelay = new SliderSetting("Delay", this, 20.0, 5.0, 60.0, 1.0);

    private int throwTimer;
    private RegistryEntry<StatusEffect> pendingEffect;

    public AutoPotion() {
        addSettings(potions, throwDelay);
    }

    @Subscribe
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        if (throwTimer > 0) {
            throwTimer--;
            if (throwTimer == 0 && pendingEffect != null) {
                if (!mc.player.hasStatusEffect(pendingEffect)) {
                    tryThrow(pendingEffect);
                }
                pendingEffect = null;
            }
            return;
        }

        if (!mc.player.getItemDropCooldown().canUse()) return;

        if (potions.isSelected("Fire Resistance") && !mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            tryThrow(StatusEffects.FIRE_RESISTANCE);
        } else if (potions.isSelected("Strength") && !mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            tryThrow(StatusEffects.STRENGTH);
        } else if (potions.isSelected("Speed") && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            tryThrow(StatusEffects.SPEED);
        }
    }

    private void tryThrow(RegistryEntry<StatusEffect> effect) {
        int slot = findPotion(effect);
        if (slot == -1) return;

        pendingEffect = effect;
        throwPotion(slot);
        throwTimer = (int) throwDelay.getFloatValue();
    }

    private int findPotion(RegistryEntry<StatusEffect> effect) {
        for (int i = 0; i < 9; i++) {
            if (matchesEffect(mc.player.getInventory().getStack(i), effect)) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (matchesEffect(mc.player.getInventory().getStack(i), effect)) return i;
        }
        return -1;
    }

    private boolean matchesEffect(ItemStack stack, RegistryEntry<StatusEffect> effect) {
        if (stack.getItem() != Items.SPLASH_POTION) return false;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        for (StatusEffectInstance instance : contents.getEffects()) {
            if (instance.getEffectType().equals(effect)) return true;
        }
        return false;
    }

    private void throwPotion(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;

        float savedYaw = mc.player.getYaw();
        float savedPitch = mc.player.getPitch();
        int savedSlot = mc.player.getInventory().selectedSlot;

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(savedYaw, 90.0f, mc.player.isOnGround(), true));
        mc.player.setYaw(90.0f);

        if (slot < 9) {
            if (slot != savedSlot) {
                mc.player.getInventory().selectedSlot = slot;
            }
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            if (slot != savedSlot) {
                mc.player.getInventory().selectedSlot = savedSlot;
            }
        } else {
            mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    slot >= 36 ? slot : slot + 36,
                    savedSlot,
                    SlotActionType.SWAP,
                    mc.player
            );
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    slot >= 36 ? slot : slot + 36,
                    savedSlot,
                    SlotActionType.SWAP,
                    mc.player
            );
        }

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(savedYaw, savedPitch, mc.player.isOnGround(),true));
        mc.player.setYaw(savedYaw);
        mc.player.setPitch(savedPitch);
    }
}

