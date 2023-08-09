package scheduling.events;

import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class Event {

    private UUID eventId;
    private long timestamp;

    protected Event() {
        this.eventId = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
    }

}