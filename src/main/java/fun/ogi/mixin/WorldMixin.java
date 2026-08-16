package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.render.WorldTweaks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {

    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        WorldTweaks wt = Cheap.getInstance().getModuleStorage().get(WorldTweaks.class);
        if (wt != null && wt.isEnabled() && wt.timeToggle.getValue()) {
            cir.setReturnValue((long) wt.timeSetting.getValue());
        }
    }
}

