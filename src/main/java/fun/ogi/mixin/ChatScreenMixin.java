package fun.ogi.mixin;

import fun.ogi.Cheap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    private void onSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (chatText.startsWith(".")) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                Cheap.getInstance().getCommandManager().handleCommand(chatText);
                if (addToHistory) {
                    client.inGameHud.getChatHud().addToMessageHistory(chatText);
                }
            }
            ci.cancel();
        }
    }
}

