package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.util.BossBarData;
import fun.ogi.module.impl.list.render.Removals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.boss.BossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Shadow
    @Final
    private Map<UUID, BossBar> bossBars;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(DrawContext context, CallbackInfo ci) {
        List<String> texts = new ArrayList<>();
        for (BossBar bar : bossBars.values()) {
            texts.add(bar.getName().getString());
        }
        BossBarData.updateBossBars(texts);

        Removals removals = Cheap.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Boss Bar")) {
            ci.cancel();
        }
    }
}

