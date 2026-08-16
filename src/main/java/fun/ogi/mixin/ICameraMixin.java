package fun.ogi.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface ICameraMixin {
    @Invoker("setRotation")
    void setCustomRotation(float yaw, float pitch);

    @Invoker("clipToSpace")
    float setClipToSpace(float distance);

    @Invoker("moveBy")
    void setCustomMoveBy(float x, float y, float z);
}

