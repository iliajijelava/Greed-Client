package fun.ogi.mixin;

import fun.ogi.Cheap;
import fun.ogi.events.PacketEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        PacketEvent event = new PacketEvent(packet, PacketEvent.Type.SEND);
        Cheap.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void onChannelRead0(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        PacketEvent event = new PacketEvent(packet, PacketEvent.Type.RECEIVE);
        Cheap.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}

