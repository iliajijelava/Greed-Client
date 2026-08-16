package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Box;

@ModuleInformation(moduleName = "AutoTotem", moduleDesc = "Automatically equips totem in offhand", moduleCategory = ModuleCategory.COMBAT)
public class AutoTotem extends Module {

    private final SliderSetting health = new SliderSetting("Health", this, 6.0, 1.0, 20.0, 0.5);
    private final SliderSetting crystalRange = new SliderSetting("Crystal Range", this, 4.0, 1.0, 6.0, 0.5);
    private final BooleanSetting fallCheck = new BooleanSetting("Fall Check", this, true);

    public AutoTotem() {
        addSettings(health, crystalRange, fallCheck);
    }

    @Subscribe
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        boolean falling =
                mc.player != null &&
                        !mc.player.isOnGround() &&
                        mc.player.getVelocity().y < 0;
        boolean needsTotem = false;

        if (mc.player.getHealth() <= health.getFloatValue()) {
            needsTotem = true;
        }

        if (fallCheck.getValue() && falling) {
            needsTotem = true;
        }

        if (crystalRange.getValue() > 0) {
            Box box = mc.player.getBoundingBox().expand(crystalRange.getValue());
            if (!mc.world.getEntitiesByClass(EndCrystalEntity.class, box, e2 -> true).isEmpty()) {
                needsTotem = true;
            }
        }

        if (!needsTotem) return;

        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        int totemSlot = findTotem();
        if (totemSlot == -1) return;

        int containerSlot = totemSlot;
        if (containerSlot >= 0 && containerSlot <= 8) {
            containerSlot += 36;
        }

        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                containerSlot,
                40,
                SlotActionType.SWAP,
                mc.player
        );
    }

    private int findTotem() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }
}

