package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.misc.NameProtect;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InGameHud.class)
public class SidebarMixin {
    @ModifyArg(
        method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"),
        index = 1
    )
    private Text modifyDrawText(Text text) {
        if (Cheap.getInstance() == null) return text;
        try {
            NameProtect np = Cheap.getInstance().getModuleStorage().get(NameProtect.class);
            if (np != null && np.isEnabled()) {
                return np.patchText(text);
            }
        } catch (Exception ignored) {
        }
        return text;
    }
}

