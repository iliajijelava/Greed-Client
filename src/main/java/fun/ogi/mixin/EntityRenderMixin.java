package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.render.EventRenderEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onRenderEntity(E entity, double x, double y, double z, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        EventRenderEntity event = new EventRenderEntity(entity, matrices, vertexConsumers, light);
        event.setPosition(x, y, z);
        Cheap.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel(); 
        }
    }
}

