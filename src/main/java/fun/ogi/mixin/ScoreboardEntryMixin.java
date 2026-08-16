package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.misc.NameProtect;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScoreboardEntry.class)
public class ScoreboardEntryMixin {
    @Inject(method = "name", at = @At("RETURN"), cancellable = true)
    private void onGetName(CallbackInfoReturnable<Text> ci) {
        Text original = ci.getReturnValue();
        if (original == null || Cheap.getInstance() == null) return;
        try {
            NameProtect np = Cheap.getInstance().getModuleStorage().get(NameProtect.class);
            if (np != null && np.isEnabled()) {
                ci.setReturnValue(np.patchText(original));
            }
        } catch (Exception ignored) {
        }
    }
}

