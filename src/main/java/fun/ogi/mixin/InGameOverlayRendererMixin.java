package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.render.Hud;
import fun.ogi.module.impl.list.render.Removals;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void greed$renderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (Cheap.getInstance().getModuleStorage() == null) return;

        Removals removals = Cheap.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Fire")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void greed$renderInWallOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (Cheap.getInstance().getModuleStorage() == null) return;

        Removals removals = Cheap.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Overlay in block")) {
            ci.cancel();
        }
    }
}

