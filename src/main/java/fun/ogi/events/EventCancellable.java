package fun.ogi.events;



public class EventCancellable extends Event {
   private boolean cancelled;

   public void cancel() {
      this.cancelled = true;
   }


   public boolean isCancelled() {
      return this.cancelled;
   }
}

