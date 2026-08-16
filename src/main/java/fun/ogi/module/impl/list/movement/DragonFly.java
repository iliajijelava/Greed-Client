package fun.ogi.module.impl.list.movement;


import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.SliderSetting;

@ModuleInformation(moduleName = "Dragon fly",moduleDesc = "Fly faster with dragon",moduleCategory = ModuleCategory.MOVEMENT)
public class DragonFly extends Module {
    private final SliderSetting speed = new SliderSetting("Speed",this,1.5f,1.0f,3.0f,0.1);

    public DragonFly() {
        addSetting(speed);
    }

    @Subscribe
    public void onTick(EventUpdate event) {
        if (mc.player == null) {
            return;
        }

        if (!mc.player.getAbilities().allowFlying) {
            mc.player.getAbilities().setFlySpeed(0.05F);
            return;
        }

        mc.player.getAbilities().setFlySpeed(isMoving()
                ? 0.05F * speed.getFloatValue() : 0.05F);
    }

    @Subscribe
    public void deactivate() {
        if (mc.player != null) {
            mc.player.getAbilities().setFlySpeed(0.05F);
        }
        super.onDisable();
    }

    private boolean isMoving() {
        return mc.player != null
                && (mc.player.input.movementForward != 0.0F || mc.player.input.movementSideways != 0.0F);
    }
}

