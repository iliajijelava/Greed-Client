package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.module.impl.list.misc.NameProtect;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void onGetPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> ci) {
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

