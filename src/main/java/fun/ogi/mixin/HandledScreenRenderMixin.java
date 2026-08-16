package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.render.EventHandledScreenRender;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenRenderMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void ogi$postHandledScreenRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Cheap.getInstance().getEventBus().post(new EventHandledScreenRender((HandledScreen) (Object) this, context));
    }
}

