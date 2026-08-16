package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.render.Chams;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;",
            at = @At("HEAD"),
            cancellable = true)
    private void onGetRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
        Chams chams = Cheap.getInstance().getModuleStorage().get(Chams.class);
        if (chams == null || !chams.isEnabled()) return;

        PlayerEntity player = resolvePlayer(state);
        if (player != null && chams.shouldHideBaseModel(player)) {
            cir.setReturnValue(null);
        }
    }

    @Unique
    private PlayerEntity resolvePlayer(LivingEntityRenderState state) {
        if (!(state instanceof PlayerEntityRenderState playerState)) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return null;
        Entity entity = mc.world.getEntityById(playerState.id);
        return entity instanceof PlayerEntity player ? player : null;
    }
}

