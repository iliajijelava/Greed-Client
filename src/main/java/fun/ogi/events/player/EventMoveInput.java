package fun.ogi.events.player;

import fun.ogi.events.Event;

public class EventMoveInput extends Event {
    private float forward;
    private float strafe;
    private boolean jump, sneak;

    public float getForward() { return forward; }
    public void setForward(float forward) { this.forward = forward; }
    public float getStrafe() { return strafe; }
    public void setStrafe(float strafe) { this.strafe = strafe; }
    public boolean isJump() { return jump; }
    public void setJump(boolean jump) { this.jump = jump; }
    public boolean isSneak() { return sneak; }
    public void setSneak(boolean sneak) { this.sneak = sneak; }
}

