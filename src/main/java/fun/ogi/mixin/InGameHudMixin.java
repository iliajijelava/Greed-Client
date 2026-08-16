package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.render.EventHud;
import fun.ogi.events.render.EventHudPre;
import fun.ogi.module.impl.list.render.Hud;
import fun.ogi.module.impl.list.render.Removals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHudPre(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud()) return;
        Cheap.getInstance().getEventBus().post(new EventHudPre(context, tickCounter));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud()) return;
        Cheap.getInstance().getEventBus().post(new EventHud(context, tickCounter));
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Hud hud = Cheap.getInstance().getModuleStorage().get(Hud.class);
        if (hud != null && hud.isEnabled() && hud.Pots.getValue()) {
            ci.cancel();
        }
    }
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void renderScoreboard(DrawContext context,RenderTickCounter tickCounter, CallbackInfo ci) {

        Removals removals = Cheap.getInstance().getModuleStorage().get(Removals.class);

        if(removals != null && removals.isEnabled("Scoreboard")) {
            ci.cancel();
        }
    }
}

