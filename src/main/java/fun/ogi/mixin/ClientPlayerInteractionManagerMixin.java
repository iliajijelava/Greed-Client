package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.AttackEvent;
import fun.ogi.module.impl.list.combat.NoInteract;
import fun.ogi.module.impl.list.player.NoEntityTrace;
import fun.ogi.module.impl.list.misc.FakePlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player != MinecraftClient.getInstance().player) return;

        
        
        if (target == FakePlayer.getFakePlayer()) {
            ci.cancel();
            player.swingHand(Hand.MAIN_HAND);
            player.attack(target);
            return;
        }

        AttackEvent event = new AttackEvent(target);
        Cheap.getInstance().getEventBus().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player != MinecraftClient.getInstance().player) return;
        NoInteract ni = Cheap.getInstance().getModuleStorage().get(NoInteract.class);
        if (ni != null && ni.isEnabled()) {
            cir.setReturnValue(ActionResult.PASS);
        }
        NoEntityTrace net = Cheap.getInstance().getModuleStorage().get(NoEntityTrace.class);
        if (net != null && net.shouldIgnoreEntityTrace()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (player != MinecraftClient.getInstance().player) return;
        NoInteract ni = Cheap.getInstance().getModuleStorage().get(NoInteract.class);
        if (ni != null && ni.isEnabled()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}

