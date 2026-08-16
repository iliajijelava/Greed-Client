package fun.ogi.events.render;


import fun.ogi.events.Event;

public class EventSwingDuration extends Event {
    private float duration;

    public EventSwingDuration(float duration) {
        this.duration = duration;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;

    }
}

