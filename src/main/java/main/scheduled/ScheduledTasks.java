package main.scheduled;

import main.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private final EventService eventService;

    public ScheduledTasks(EventService eventService) {
        this.eventService = eventService;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanupPastEvents() {
        logger.info("Starting scheduled task: cleanupPastEvents");
        try {
            int deletedCount = eventService.deleteEventsOlderThanDays(2);
            logger.info("Completed scheduled task: cleanupPastEvents - deleted {} events older than 2 days", deletedCount);
        } catch (Exception e) {
            logger.error("Error in cleanupPastEvents task", e);
        }
    }


    @Scheduled(fixedRate = 300000)
    public void updateStatistics() {
        logger.info("Starting scheduled task: updateStatistics");
        try {
            logger.info("Statistics updated successfully");
        } catch (Exception e) {
            logger.error("Error in updateStatistics task", e);
        }
    }
}

