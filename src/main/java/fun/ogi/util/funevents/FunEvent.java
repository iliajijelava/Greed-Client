package fun.ogi.util.funevents;

import static fun.ogi.util.funevents.FunEventsUtil.translateEventId;
import static fun.ogi.util.funevents.FunEventsUtil.translatePhase;

public class FunEvent {
    private final String server;
    private final String eventType;
    private final String id;
    private final int timeLeftSeconds;
    private final String phase;
    private final String loot;
    private final boolean locationAnnounced;
    private final FunEventLocation location;

    public FunEvent(String server, String eventType, String id, int timeLeftSeconds, String phase,
                    String loot, boolean locationAnnounced, FunEventLocation location) {
        this.server = server;
        this.eventType = eventType;
        this.id = id;
        this.timeLeftSeconds = timeLeftSeconds;
        this.phase = phase;
        this.loot = loot;
        this.locationAnnounced = locationAnnounced;
        this.location = location;
    }

    public String getServer() {
        return server;
    }

    public String getEventType() {
        return eventType;
    }

    public String getId() {
        return id;
    }

    public String getIdRu() {
        return translateEventId(id);
    }

    public int getTimeLeftSeconds() {
        return timeLeftSeconds;
    }

    public String getTimeLeftFormatted() {
        if (timeLeftSeconds <= 0) return "сейчас";
        int minutes = timeLeftSeconds / 60;
        int seconds = timeLeftSeconds % 60;
        return minutes > 0 ? minutes + " мин " + seconds + " сек" : seconds + " сек";
    }

    public String getPhase() {
        return phase;
    }

    public String getPhaseRu() {
        return translatePhase(phase);
    }

    public String getLoot() {
        return loot;
    }

    public boolean isLocationAnnounced() {
        return locationAnnounced;
    }

    public FunEventLocation getLocation() {
        return location;
    }

    public boolean hasCoordinates() {
        return location != null && locationAnnounced;
    }

    @Override
    public String toString() {
        String anarchy = server != null ? server.replaceAll("[^0-9]", "") : "?";
        String coords = hasCoordinates() ? location.asString() : "не объявлены";
        return "Ивент: " + getIdRu()
                + " | Анархия: " + anarchy
                + " | Время: " + getTimeLeftFormatted()
                + " | Фаза: " + getPhaseRu()
                + " | Лут: " + (loot == null || loot.isEmpty() || loot.equals("null") ? "—" : loot)
                + " | Координаты: " + coords;
    }
}

