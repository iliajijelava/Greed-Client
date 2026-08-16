package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.render.ShaderESP;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class ShaderESPTeamColorMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        ShaderESP shaderESP = Cheap.getInstance().getModuleStorage().get(ShaderESP.class);
        if (shaderESP == null || !shaderESP.isEnabled()) return;

        Entity entity = (Entity) (Object) this;
        if (!shaderESP.shouldOutline(entity)) return;

        int color = shaderESP.getOutlineColor();
        cir.setReturnValue(color);
    }
}

