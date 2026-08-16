package fun.ogi.events.network;


import fun.ogi.events.Event;
import net.minecraft.network.packet.Packet;

public class ReceivePacketEvent extends Event {
   private final Packet<?> packet;


   public Packet<?> getPacket() {
      return this.packet;
   }


   public ReceivePacketEvent(Packet<?> packet) {
      this.packet = packet;
   }
}

