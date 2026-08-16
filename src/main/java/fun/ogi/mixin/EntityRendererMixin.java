package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.misc.NameProtect;
import fun.ogi.module.impl.list.render.Nametags;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    private static boolean patchingLabel = false;

    @Shadow
    protected void renderLabelIfPresent(EntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {}

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(EntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (patchingLabel || Cheap.getInstance() == null) return;
        try {
            if (state instanceof PlayerEntityRenderState && Cheap.getInstance().getModuleStorage().get(Nametags.class).isEnabled()) {
                ci.cancel();
                return;
            }
            NameProtect np = Cheap.getInstance().getModuleStorage().get(NameProtect.class);
            if (np != null && np.isEnabled()) {
                patchingLabel = true;
                Text patched = np.patchText(text);
                if (patched != null && !patched.getString().equals(text.getString())) {
                    ci.cancel();
                    renderLabelIfPresent(state, patched, matrices, vertexConsumers, light);
                }
                patchingLabel = false;
            }
        } catch (Exception e) {
            patchingLabel = false;
        }
    }
}

