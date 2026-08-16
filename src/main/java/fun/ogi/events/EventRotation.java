package fun.ogi.events;

public class EventRotation extends Event {
    private float yaw, pitch;
    private final float partialTicks;

    public EventRotation(float yaw, float pitch, float partialTicks) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.partialTicks = partialTicks;
    }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public float getPartialTicks() { return partialTicks; }
}

