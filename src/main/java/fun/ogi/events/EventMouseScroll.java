package fun.ogi.events;

public class EventMouseScroll {
    private final double scrollDelta;

    public EventMouseScroll(double scrollDelta) {
        this.scrollDelta = scrollDelta;
    }

    public double getScrollDelta() {
        return scrollDelta;
    }
}