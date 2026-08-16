package fun.ogi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fun.ogi.module.impl.list.render.SwingAnimations;
import fun.ogi.util.compat.HMICompat;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;















@Mixin(value = HeldItemRenderer.class, priority = 1100)
public abstract class HeldItemRendererHMIMixin {

    @WrapOperation(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    private void onRenderFirstPersonItem(
            HeldItemRenderer instance,
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            Operation<Void> original
    ) {
        if (HMICompat.shouldUseHMI()) {
            original.call(instance, player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
            return;
        }

        SwingAnimations tweaks = SwingAnimations.INSTANCE;

        Hand renderHand = hand;

        if (tweaks != null
                && tweaks.isEnabled()
                && tweaks.swapHands.getValue()) {

            renderHand = hand == Hand.MAIN_HAND
                    ? Hand.OFF_HAND
                    : Hand.MAIN_HAND;
        }

        ((HeldItemRendererInvoker) instance).invokeRenderFirstPersonItem(
                player,
                tickDelta,
                pitch,
                renderHand,
                swingProgress,
                item,
                equipProgress,
                matrices,
                vertexConsumers,
                light
        );
    }
}

