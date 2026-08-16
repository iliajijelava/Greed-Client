package fun.ogi.events.render;

import fun.ogi.events.Event;

import java.awt.Color;

public class EventFog extends Event {
    private float distance;
    private Color fogColor;

    public EventFog(float distance) {
        this.distance = distance;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public Color getFogColor() {
        return fogColor;
    }

    public void setFogColor(Color fogColor) {
        this.fogColor = fogColor;
    }

    public boolean hasFogColor() {
        return fogColor != null;
    }
}

