package fun.ogi.events;

import net.minecraft.network.packet.Packet;

public class PacketEvent extends Event {
    Packet<?> packet;
    Type type;

    public PacketEvent(Packet<?> packet, Type type) {
        this.packet = packet;
        this.type = type;
    }

    public boolean isSend() {
        return type.equals(Type.SEND);
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        SEND, RECEIVE
    }
    public Packet<?> getPacket() {
        return packet;
    }
}

