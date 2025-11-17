package main.scheduler;

import main.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EventCleanupScheduler.class);
    private final EventService eventService;
    private static final int DAYS_TO_KEEP = 30; // Изтрива събития по-стари от 30 дни

    public EventCleanupScheduler(EventService eventService) {
        this.eventService = eventService;
    }

    @Scheduled(cron = "0 0 2 * * ?") // Изпълнява се всеки ден в 02:00 сутринта
    public void cleanupOldEvents() {
        try {
            logger.info("Започва автоматично изтриване на стари събития (по-стари от {} дни)...", DAYS_TO_KEEP);
            int deletedCount = eventService.deleteEventsOlderThanDays(DAYS_TO_KEEP);
            logger.info("Автоматично изтриване завършено. Изтрити са {} събития.", deletedCount);
        } catch (Exception e) {
            logger.error("Грешка при автоматично изтриване на стари събития", e);
        }
    }
}

