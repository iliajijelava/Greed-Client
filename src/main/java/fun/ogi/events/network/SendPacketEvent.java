package fun.ogi.events.network;


import fun.ogi.events.Event;
import net.minecraft.network.packet.Packet;

public class SendPacketEvent extends Event {
   private Packet<?> packet;


   public Packet<?> getPacket() {
      return this.packet;
   }


   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }


   public SendPacketEvent(Packet<?> packet) {
      this.packet = packet;
   }
}

