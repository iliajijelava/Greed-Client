package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.util.ChatData;
import fun.ogi.module.impl.list.misc.NameProtect;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    private static boolean patching = false;

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        ChatData.addMessage(message.getString());

        if (patching || Cheap.getInstance() == null) return;
        try {
            NameProtect np = Cheap.getInstance().getModuleStorage().get(NameProtect.class);
            if (np != null && np.isEnabled()) {
                patching = true;
                Text patched = np.patchText(message);
                if (patched != null && !patched.getString().equals(message.getString())) {
                    ((ChatHud) (Object) this).addMessage(patched, signature, indicator);
                    ci.cancel();
                }
                patching = false;
            }
        } catch (Exception e) {
            patching = false;
        }
    }
}

