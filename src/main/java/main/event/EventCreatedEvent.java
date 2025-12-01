package main.event;

import main.model.Event;
import org.springframework.context.ApplicationEvent;

public class EventCreatedEvent extends ApplicationEvent {
    private final Event event;

    public EventCreatedEvent(Object source, Event event) {
        super(source);
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
}

