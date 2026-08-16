package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.mixin.LivingEntityAccessor;
import fun.ogi.mixin.MinecraftClientAccessor;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;

@ModuleInformation(moduleName = "NoDelay", moduleDesc = "Removes various delays", moduleCategory = ModuleCategory.PLAYER)
public class NoDelay extends Module {

    public BooleanSetting jump = new BooleanSetting("Jump Delay", this, true);
    public BooleanSetting rClick = new BooleanSetting("Right Click", this, false);

    public NoDelay() {
        addSetting(jump);
        addSetting(rClick);
    }

    @Subscribe
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (jump.getValue()) ((LivingEntityAccessor) mc.player).setJumpingCooldown(0);
        if (rClick.getValue()) ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
    }
}