package fun.ogi.mixin;

import fun.ogi.module.impl.list.render.TrollfaceMaskRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Shadow
    protected List<FeatureRenderer<?, ?>> features;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void cheap$addTrollfaceMask(net.minecraft.client.render.entity.EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        try {
            PlayerEntityRenderer self = (PlayerEntityRenderer) (Object) this;
            features.add(new TrollfaceMaskRenderer(self));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

