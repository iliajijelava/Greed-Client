package fun.ogi.module.impl.list.render;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import static fun.ogi.util.MinecraftUtil.mc;

@ModuleInformation(moduleName = "Full Bright", moduleCategory = ModuleCategory.RENDER)
public class FullBright extends Module {
    @Subscribe
    public void onUpdate(EventUpdate e){
        if(mc.player == null || mc.world == null) return;
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0, false, false));
    }
}

