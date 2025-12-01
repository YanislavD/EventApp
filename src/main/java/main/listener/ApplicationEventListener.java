package main.listener;

import main.event.EventCreatedEvent;
import main.event.EventDeletedEvent;
import main.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationEventListener.class);
    
    @EventListener
    public void handleEventCreated(EventCreatedEvent event) {
        logger.info("Event created: {} by user {}", 
            event.getEvent().getName(), 
            event.getEvent().getCreator().getEmail());
    }
    
    @EventListener
    public void handleEventDeleted(EventDeletedEvent event) {
        logger.info("Event deleted: {} (ID: {})", 
            event.getEventName(), 
            event.getEventId());
    }
    
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        logger.info("New user registered: {} ({})", 
            event.getUser().getEmail(), 
            event.getUser().getUsername());
    }
}

