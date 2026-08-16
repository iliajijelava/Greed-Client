package fun.ogi.events;

public class Event {
    public boolean cancelled ;
    public boolean isCancelled(){
        return cancelled;
    }
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void cancelEvent() {
        setCancelled(true);
    }
}

