package fun.ogi.events;

public class EventLook extends Event {
    private double yaw, pitch;

    public EventLook(double yaw, double pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getYaw() { return yaw; }
    public void setYaw(double yaw) { this.yaw = yaw; }
    public double getPitch() { return pitch; }
    public void setPitch(double pitch) { this.pitch = pitch; }
}

