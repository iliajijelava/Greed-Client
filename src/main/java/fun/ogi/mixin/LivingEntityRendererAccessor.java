package fun.ogi.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    @Invoker("setupTransforms")
    void invokeSetupTransforms(LivingEntityRenderState state, MatrixStack matrices, float bodyYaw, float baseScale);

    @Invoker("scale")
    void invokeScale(LivingEntityRenderState state, MatrixStack matrices);
}

