package fun.ogi.events.render;

import fun.ogi.events.Event;

public class EventFrame extends Event {
    private final float deltaTime;

    public EventFrame(float deltaTime) {
        this.deltaTime = deltaTime;
    }

    public float getDeltaTime() {
        return deltaTime;
    }
}

