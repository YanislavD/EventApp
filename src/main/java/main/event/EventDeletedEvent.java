package main.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class EventDeletedEvent extends ApplicationEvent {
    private final UUID eventId;
    private final String eventName;

    public EventDeletedEvent(Object source, UUID eventId, String eventName) {
        super(source);
        this.eventId = eventId;
        this.eventName = eventName;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }
}

