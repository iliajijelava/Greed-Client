package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.BoundingBoxControlEvent;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.SliderSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

@ModuleInformation(moduleName = "HitBox", moduleDesc = "Expands entity hitboxes", moduleCategory = ModuleCategory.COMBAT)
public class HitBox extends Module {

    private final SliderSetting xzExpand = new SliderSetting("XZ Expand", this, 0.2, 0.0, 3.0, 0.05);

    public HitBox() {
        addSettings(xzExpand);
    }

    @Subscribe
    public void onBoundingBoxControl(BoundingBoxControlEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            if (living == mc.player) return;
            if (Cheap.getInstance().getFriendManager().contains(living.getName().getString())) return;

            Box box = event.getBox();
            float xz = xzExpand.getFloatValue();

            Box expanded = new Box(
                    box.minX - xz / 2.0, box.minY - 0.0 / 2.0, box.minZ - xz / 2.0,
                    box.maxX + xz / 2.0, box.maxY + 0.0 / 2.0, box.maxZ + xz / 2.0
            );

            event.setBox(expanded);
        }
    }
}

