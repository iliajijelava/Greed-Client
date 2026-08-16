package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(
        moduleName = "AntiBot",
        moduleDesc = "Detects bots on the server",
        moduleCategory = ModuleCategory.COMBAT
)
public class AntiBot extends Module {

    public static final List<Entity> isBot = new ArrayList<>();

    public AntiBot() {
        addSettings();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        updateBotList();
    }

    public void updateBotList() {
        if (mc.world == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (mc.player != player
                    && player.getInventory().armor.get(0).getItem() != Items.AIR
                    && player.getInventory().armor.get(1).getItem() != Items.AIR
                    && player.getInventory().armor.get(2).getItem() != Items.AIR
                    && player.getInventory().armor.get(3).getItem() != Items.AIR
                    && player.getInventory().armor.get(0).isEnchantable()
                    && player.getInventory().armor.get(1).isEnchantable()
                    && player.getInventory().armor.get(2).isEnchantable()
                    && player.getInventory().armor.get(3).isEnchantable()
                    && player.getOffHandStack().getItem() == Items.AIR
                    && (player.getInventory().armor.get(0).getItem() == Items.LEATHER_BOOTS
                    || player.getInventory().armor.get(1).getItem() == Items.LEATHER_LEGGINGS
                    || player.getInventory().armor.get(2).getItem() == Items.LEATHER_CHESTPLATE
                    || player.getInventory().armor.get(3).getItem() == Items.LEATHER_HELMET
                    || player.getInventory().armor.get(0).getItem() == Items.IRON_BOOTS
                    || player.getInventory().armor.get(1).getItem() == Items.IRON_LEGGINGS
                    || player.getInventory().armor.get(2).getItem() == Items.IRON_CHESTPLATE
                    || player.getInventory().armor.get(3).getItem() == Items.IRON_HELMET)
                    && player.getMainHandStack().getItem() != Items.AIR
                    && !player.getInventory().armor.get(0).isDamaged()
                    && !player.getInventory().armor.get(1).isDamaged()
                    && !player.getInventory().armor.get(2).isDamaged()
                    && !player.getInventory().armor.get(3).isDamaged()
                    && player.getHungerManager().getFoodLevel() == 20) {
                if (!isBot.contains(player)) {
                    isBot.add(player);
                }
                return;
            }
            isBot.remove(player);
        }
    }

    public static boolean checkBot(LivingEntity entity) {
        return entity instanceof PlayerEntity && isBot.contains(entity);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isBot.clear();
    }
}

