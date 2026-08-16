package fun.ogi.events.player;

import fun.ogi.events.EventCancellable;

public class EventCloseInv extends EventCancellable {
    private final int syncId;

    public EventCloseInv(int syncId) {
        this.syncId = syncId;
    }

    public int getSyncId() {
        return syncId;
    }
}

