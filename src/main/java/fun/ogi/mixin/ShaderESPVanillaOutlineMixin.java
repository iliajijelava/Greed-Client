package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.render.ShaderESP;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class ShaderESPVanillaOutlineMixin {
    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At("HEAD"), cancellable = true)
    private void onDrawEntityOutlinesFramebuffer(CallbackInfo ci) {
        ShaderESP shaderESP = Cheap.getInstance().getModuleStorage().get(ShaderESP.class);
        if (shaderESP != null && shaderESP.isEnabled()) {
            ci.cancel();
        }
    }
}

