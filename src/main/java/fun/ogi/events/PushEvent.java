package fun.ogi.events;

public class PushEvent extends Event {
    private final Type type;

    public PushEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        WATER,
        BLOCK,
        COLLISION
    }
}

