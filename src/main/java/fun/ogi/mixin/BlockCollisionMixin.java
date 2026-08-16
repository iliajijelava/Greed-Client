package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.PlayerCollisionEvent;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlock.class)
public class BlockCollisionMixin {

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void onOnEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity)) return;
        if (entity != MinecraftClient.getInstance().player) return;

        Block block = state.getBlock();
        if (block instanceof PowderSnowBlock || block instanceof SweetBerryBushBlock) {
            PlayerCollisionEvent event = new PlayerCollisionEvent(block);
            Cheap.getInstance().getEventBus().post(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}

