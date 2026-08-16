
package fun.ogi.mixin;

import fun.ogi.mixin.HeldItemRendererInvoker;
import fun.ogi.module.impl.list.render.HandFireRenderer;
import fun.ogi.module.impl.list.render.SwingAnimations;
import fun.ogi.module.impl.list.render.ViewModel;
import fun.ogi.util.render.hand.ShaderHandsRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeldItemRenderer.class, priority = 1100)
public abstract class HeldItemRendererMixin {

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRenderFirstPersonItem(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        ViewModel viewModel = ViewModel.INSTANCE;

        if (viewModel != null && viewModel.isEnabled()) {
            Arm arm = hand == Hand.MAIN_HAND
                    ? player.getMainArm()
                    : player.getMainArm().getOpposite();

            if (arm == Arm.RIGHT) {
                matrices.translate(
                        viewModel.rightX.getFloatValue(),
                        viewModel.rightY.getFloatValue(),
                        -viewModel.rightZ.getFloatValue() * 0.3f
                );
            } else {
                matrices.translate(
                        viewModel.leftX.getFloatValue(),
                        viewModel.leftY.getFloatValue(),
                        -viewModel.leftZ.getFloatValue() * 0.3f
                );
            }
        }
    }


    









    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
                    ordinal = 2
            )
    )
    private void onSwingArm(
            HeldItemRenderer instance,
            float swingProgress,
            float equipProgress,
            MatrixStack matrices,
            int armX,
            Arm arm
    ) {
        SwingAnimations tweaks = SwingAnimations.INSTANCE;

        if (tweaks == null
                || !tweaks.isEnabled()
                || !tweaks.swingEnabled.getValue()) {

            callSwingArm(
                    swingProgress,
                    equipProgress,
                    matrices,
                    armX,
                    arm
            );

            return;
        }

        





        if ("HMI".equals(tweaks.swingType.getValue())) {
            callSwingArm(
                    swingProgress,
                    equipProgress,
                    matrices,
                    armX,
                    arm
            );

            return;
        }

        if (mc().player != null) {
            Arm expectedArm = mc().player.getMainArm();

            if (arm != expectedArm) {
                callSwingArm(
                        swingProgress,
                        equipProgress,
                        matrices,
                        armX,
                        arm
                );

                return;
            }
        }

        int i = arm == Arm.RIGHT ? 1 : -1;

        float strength = tweaks.swingStrength.getFloatValue();

        float sin1 = MathHelper.sin(
                swingProgress * swingProgress * (float) Math.PI
        );

        float sin2 = MathHelper.sin(
                MathHelper.sqrt(swingProgress) * (float) Math.PI
        );

        switch (tweaks.swingType.getValue()) {

            case "Down" -> {
                matrices.translate(i * 0.56f, -0.32f, -0.72f);
                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(76 * i)
                );
                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                sin2 * -5 * strength
                        )
                );
                matrices.multiply(
                        RotationAxis.NEGATIVE_X.rotationDegrees(
                                sin2 * -100 * strength
                        )
                );
                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                sin2 * -155 * strength
                        )
                );
                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(-100)
                );
            }

            case "Poke" -> {
                float pokeAnim =
                        (float) Math.sin(
                                swingProgress * (Math.PI / 2) * 2
                        );

                float tilt = strength / 3f;

                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.translate(
                        0.0f,
                        0.0f,
                        tilt * -pokeAnim
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(75f * i)
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Z.rotationDegrees(
                                (-75f * (strength / 4f) * pokeAnim - 60f) * i
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(-75f)
                );
            }

            case "Static" -> {
                matrices.translate(i * 0.56f, -0.42f, -0.72f);

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                sin2 * -60f * strength
                        )
                );

                matrices.translate(0, -0.1, 0);
            }

            case "Feast" -> {
                matrices.translate(i * 0.56f, -0.32f, -0.72f);

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(30 * i)
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                sin2 * 75 * i * strength
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                sin2 * -65 * strength
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(30 * i)
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(-80)
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(35 * i)
                );
            }

            case "Akrien" -> {
                matrices.translate(i * 0.65f, -0.32f, -0.72f);

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(76 * i)
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                sin2 * -5 * strength
                        )
                );

                matrices.multiply(
                        RotationAxis.NEGATIVE_X.rotationDegrees(
                                sin2 * -100 * strength
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                sin2 * -155 * strength
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(-100)
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                sin2 * 25 * strength
                        )
                );

                matrices.multiply(
                        RotationAxis.NEGATIVE_X.rotationDegrees(
                                sin2 * -25 * strength
                        )
                );

                matrices.multiply(
                        RotationAxis.NEGATIVE_X.rotationDegrees(
                                sin1 * 15 * strength
                        )
                );

                matrices.translate(
                        sin2 * 0.18f * strength,
                        sin2 * 0.59f * strength,
                        0
                );
            }

            case "Smooth" ->
                    applySwingOffset(
                            matrices,
                            i,
                            swingProgress,
                            strength
                    );

            case "Block" -> {
                if (swingProgress > 0) {
                    float g = MathHelper.sin(
                            MathHelper.sqrt(swingProgress)
                                    * (float) Math.PI
                    );

                    matrices.translate(
                            0.56f * i,
                            equipProgress * -0.2f - 0.5f,
                            -0.7f
                    );

                    matrices.multiply(
                            RotationAxis.POSITIVE_Y.rotationDegrees(
                                    45 * i
                            )
                    );

                    matrices.multiply(
                            RotationAxis.POSITIVE_X.rotationDegrees(
                                    g * -85f * strength
                            )
                    );

                    matrices.translate(
                            -0.1f * i,
                            0.28f,
                            0.2f
                    );

                    matrices.multiply(
                            RotationAxis.POSITIVE_X.rotationDegrees(-85f)
                    );

                } else {
                    float n =
                            -0.4f
                                    * MathHelper.sin(
                                    MathHelper.sqrt(swingProgress)
                                            * (float) Math.PI
                            );

                    float m =
                            0.2f
                                    * MathHelper.sin(
                                    MathHelper.sqrt(swingProgress)
                                            * ((float) Math.PI * 2)
                            );

                    float f1 =
                            -0.2f
                                    * MathHelper.sin(
                                    swingProgress * (float) Math.PI
                            );

                    matrices.translate(
                            n * i * strength,
                            m * strength,
                            f1 * strength
                    );

                    applyEquipOffset(
                            matrices,
                            i,
                            equipProgress
                    );

                    applySwingOffset(
                            matrices,
                            i,
                            swingProgress,
                            strength
                    );
                }
            }

            case "ToBack" -> {
                float toBackG =
                        MathHelper.sin(
                                MathHelper.sqrt(swingProgress)
                                        * (float) Math.PI
                        );

                matrices.translate(
                        0.65f * i,
                        -0.45f,
                        -0.9f
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(50f)
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                (-30f
                                        * (1f - toBackG * strength)
                                        - 30f) * i
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Z.rotationDegrees(
                                110f * i
                        )
                );
            }

            case "SelfBack" -> {
                float selfBackAnim =
                        (float) Math.sin(
                                swingProgress
                                        * (Math.PI / 2)
                                        * 2
                        );

                matrices.translate(
                        0.65f * i,
                        -0.3f,
                        -0.8f
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                90 * i
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Z.rotationDegrees(
                                -70 * i
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                -100
                                        - (60 * strength) * selfBackAnim
                        )
                );
            }

            case "Break" -> {
                matrices.translate(
                        0.66F * i,
                        -0.3F,
                        -0.38F
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                270 * i
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                sin2 * 10F * strength
                        )
                );

                matrices.scale(
                        0.5F,
                        0.5F,
                        0.5F
                );

                matrices.translate(
                        -0.1F * i,
                        0.2F,
                        0.0F
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                -10.0F * i
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(90.0F)
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                -105F * i
                        )
                );
            }

            case "DropDown" -> {
                float dropAnim =
                        (float) Math.sin(
                                swingProgress
                                        * (Math.PI / 2)
                                        * 2
                        );

                applyEquipOffset(
                        matrices,
                        i,
                        0
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(80f)
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                tweaks.corner.getFloatValue()
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                -tweaks.slant.getFloatValue()
                                        * dropAnim
                                        * strength
                        )
                );
            }

            case "Pander" -> {
                float panderAnim =
                        MathHelper.sin(
                                swingProgress * (float) Math.PI
                        );

                float panderF =
                        1f - equipProgress;

                matrices.translate(
                        i * 0.56f,
                        -0.52f,
                        -0.72f
                );

                matrices.translate(
                        (0.3f - panderAnim * 0.15f) * i,
                        0.2f - panderF * 0.12f,
                        -0.15f - panderAnim * 0.13f
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Y.rotationDegrees(
                                (76f - 10f * panderAnim) * i
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Z.rotationDegrees(
                                (-16f - 8f * panderAnim) * i
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                -83f - 26f * panderAnim
                        )
                );
            }

            case "Slant" -> {
                float slantAnim =
                        (float) Math.sin(
                                swingProgress
                                        * (Math.PI / 2.0)
                                        * 2.0
                        );

                float rotate =
                        35.0f * strength;

                matrices.translate(
                        i * 0.56f,
                        -0.52f,
                        -0.72f
                );

                matrices.translate(
                        0.0f,
                        0.0f,
                        -0.3f * slantAnim * strength
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_X.rotationDegrees(
                                slantAnim * -rotate
                        )
                );

                matrices.multiply(
                        RotationAxis.POSITIVE_Z.rotationDegrees(
                                slantAnim * rotate
                        )
                );
            }

            case "HMI" -> {
                



                callSwingArm(
                        swingProgress,
                        equipProgress,
                        matrices,
                        armX,
                        arm
                );
            }

            default ->
                    callSwingArm(
                            swingProgress,
                            equipProgress,
                            matrices,
                            armX,
                            arm
                    );
        }
    }


    @Unique
    private void applyEquipOffset(
            MatrixStack matrices,
            int i,
            float equipProgress
    ) {
        matrices.translate(
                i * 0.56f,
                -0.52f + equipProgress * -0.6f,
                -0.72f
        );
    }


    @Unique
    private void applySwingOffset(
            MatrixStack matrices,
            int i,
            float swingProgress,
            float strength
    ) {
        float f =
                MathHelper.sin(
                        swingProgress
                                * swingProgress
                                * (float) Math.PI
                );

        matrices.translate(
                0.56f * i,
                -0.52f,
                -0.72f
        );

        matrices.multiply(
                RotationAxis.POSITIVE_Y.rotationDegrees(
                        i * (45f + f * -20f * strength)
                )
        );

        float g =
                MathHelper.sin(
                        MathHelper.sqrt(swingProgress)
                                * (float) Math.PI
                );

        matrices.multiply(
                RotationAxis.POSITIVE_Z.rotationDegrees(
                        i * g * -20f * strength
                )
        );

        matrices.multiply(
                RotationAxis.POSITIVE_X.rotationDegrees(
                        g * -80f * strength
                )
        );

        matrices.multiply(
                RotationAxis.POSITIVE_Y.rotationDegrees(
                        i * -45f
                )
        );
    }


    @Unique
    private void callSwingArm(
            float swingProgress,
            float equipProgress,
            MatrixStack matrices,
            int armX,
            Arm arm
    ) {
        ((HeldItemRendererInvoker) this).invokeSwingArm(
                swingProgress,
                equipProgress,
                matrices,
                armX,
                arm
        );
    }


    @Unique
    private static net.minecraft.client.MinecraftClient mc() {
        return net.minecraft.client.MinecraftClient.getInstance();
    }


    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD")
    )
    private void onRenderItemHead(
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate vertexConsumers,
            ClientPlayerEntity player,
            int light,
            CallbackInfo ci
    ) {
        ShaderHandsRenderer.getInstance().captureBeforeHands();
        HandFireRenderer.getInstance().captureSceneBeforeHands();
    }


    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("RETURN")
    )
    private void onRenderItemReturn(
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate vertexConsumers,
            ClientPlayerEntity player,
            int light,
            CallbackInfo ci
    ) {
        ShaderHandsRenderer.getInstance().captureAfterHands();
        HandFireRenderer.getInstance().captureSceneAfterHands();
    }
}