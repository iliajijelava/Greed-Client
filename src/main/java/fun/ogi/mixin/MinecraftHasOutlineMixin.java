package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.render.ShaderESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftHasOutlineMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void onHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ShaderESP shaderESP = Cheap.getInstance().getModuleStorage().get(ShaderESP.class);
        if (shaderESP != null && shaderESP.isEnabled() && shaderESP.shouldOutline(entity)) {
            cir.setReturnValue(true);
        }
    }
}

