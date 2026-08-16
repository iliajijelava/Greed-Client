package fun.ogi.events;

import fun.ogi.Cheap;

public class EventManager {
    public static void call(Event event) {
        Cheap.getInstance().getEventBus().post(event);
    }
}

