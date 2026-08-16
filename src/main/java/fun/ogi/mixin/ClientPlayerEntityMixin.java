package fun.ogi.mixin;

import com.mojang.authlib.GameProfile;
import fun.ogi.Cheap;
import fun.ogi.events.network.EventUpdatePost;
import fun.ogi.events.player.*;
import fun.ogi.module.impl.list.movement.FreeCam;
import fun.ogi.module.impl.list.player.LockSlot;
import fun.ogi.module.impl.list.player.NoPush;
import fun.ogi.util.player.ViaProtocolUtils;
import fun.ogi.util.rotation.FreeLookComponent;
import fun.ogi.util.rotation.Rotation;
import fun.ogi.util.rotation.RotationComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {

    public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;

    @Shadow
    public abstract void closeScreen();

    @Inject(method = "tick", at = @At(value = "HEAD", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V"))
    private void onTick(CallbackInfo ci) {
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickPost(CallbackInfo ci) {
        Cheap.getInstance().getEventBus().post(new EventUpdatePost());

        if (shouldSyncRotation()) {
            float targetYaw, targetPitch;
            RotationComponent rotationComponent = RotationComponent.getInstance();
            if (rotationComponent.isRotating()) {
                Rotation activeRot = rotationComponent.getCurrentRotation();
                targetYaw = activeRot.getYaw();
                targetPitch = activeRot.getPitch();
            } else if (FreeLookComponent.isActive()) {
                targetYaw = FreeLookComponent.getFreeYaw();
                targetPitch = FreeLookComponent.getFreePitch();
            } else {
                targetYaw = this.getYaw();
                targetPitch = this.getPitch();
            }
            this.headYaw = targetYaw;
            this.prevHeadYaw = targetYaw;
            this.bodyYaw = targetYaw;
            this.prevBodyYaw = targetYaw;
        }
    }

    @Unique
    private boolean shouldSyncRotation() {
        return FreeLookComponent.isActive() || RotationComponent.getInstance().isRotating();
    }

    @Redirect(
            method = "tickMovement",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z", ordinal = 1),
            require = 0
    )
    private boolean onSprintKeyPressed(KeyBinding instance) {
        if (ViaProtocolUtils.isTargetProtocolBelowOneNineteen() && (this.horizontalCollision || this.collidedSoftly)) {
            return false;
        }

        EventSprint event = new EventSprint();
        Cheap.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            return false;
        }
        return instance.isPressed();
    }

    @Redirect(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
            ),
            require = 0
    )
    private boolean onSlowDownRedirect(ClientPlayerEntity player) {
        if (player.isUsingItem()) {
            EventSlowWalking event = new EventSlowWalking();
            Cheap.getInstance().getEventBus().post(event);
            return player.isUsingItem() && player.getVehicle() == null && !event.isCancelled();
        }
        return player.isUsingItem() && player.getVehicle() == null;
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    public void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        NoPush noPush = Cheap.getInstance().getModuleStorage().get(NoPush.class);
        if (noPush != null && noPush.isEnabled() && noPush.ignoreSetting.isSelected("Block")) {
            ci.cancel();
        }
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMoveHook(MovementType movementType, Vec3d movement, @NotNull CallbackInfo ci) {
        EventMove event = new EventMove(movement);
        Cheap.getInstance().getEventBus().post(event);

        if (!event.isCancelled() && event.getMovePos().equals(movement)) {
            return;
        }

        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        double d = this.getX();
        double e = this.getZ();
        super.move(movementType, event.getMovePos());
        float f = (float) Math.sqrt(Math.pow(this.getX() - d, 2) + Math.pow(this.getZ() - e, 2));
        this.updateLimbs(f);
        ci.cancel();
    }

    @Inject(method = "closeHandledScreen", at = @At("HEAD"), cancellable = true)
    private void onCloseHandledScreen(CallbackInfo ci) {
        int syncId = this.currentScreenHandler.syncId;
        EventCloseInv event = new EventCloseInv(syncId);
        Cheap.getInstance().getEventBus().post(event);
        if (!event.isCancelled()) {
            this.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
        }
        this.closeScreen();
        ci.cancel();
    }

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        LockSlot lockSlot = Cheap.getInstance().getModuleStorage().get(LockSlot.class);
        if (lockSlot != null && lockSlot.isCurrentSlotLockedForDrop()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovementHead(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.player.input == null) return;

        
        FreeCam freeCam = Cheap.getInstance().getModuleStorage().get(FreeCam.class);
        if (freeCam != null && freeCam.isEnabled()) {
            mc.player.noClip = true;
        }

        EventMoveInput event = new EventMoveInput();
        event.setForward(mc.player.input.movementForward);
        event.setStrafe(mc.player.input.movementSideways);
        Cheap.getInstance().getEventBus().post(event);

        mc.player.input.movementForward = event.getForward();
        mc.player.input.movementSideways = event.getStrafe();
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onTickMovementPost(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !mc.player.isGliding()) {
            return;
        }

        EventOnTravelPost event = new EventOnTravelPost(mc.player.getVelocity());
        Cheap.getInstance().getEventBus().post(event);
        mc.player.setVelocity(event.getOldVelocity());
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onPostMotion(CallbackInfo ci) {
        Cheap.getInstance().getEventBus().post(new EventPostMotion());
    }
}

