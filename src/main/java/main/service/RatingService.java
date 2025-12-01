package main.service;

import main.model.Event;
import main.client.RatingClient;
import main.web.dto.EventRatingSummaryResponse;
import main.web.dto.RatingRequest;
import main.web.dto.RatingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import feign.FeignException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RatingService {

    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);
    private final RatingClient ratingClient;
    private final EventService eventService;
    private final SubscriptionService subscriptionService;

    public RatingService(RatingClient ratingClient, EventService eventService, SubscriptionService subscriptionService) {
        this.ratingClient = ratingClient;
        this.eventService = eventService;
        this.subscriptionService = subscriptionService;
    }

    public RatingResponse createRating(UUID eventId, UUID userId, Integer score) {
        Event event = eventService.getById(eventId);

        if (event.getEndTime() == null || event.getEndTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Можеш да оцениш само минали събития");
        }

        if (!subscriptionService.existsByUserAndEvent(userId, event.getId())) {
            throw new IllegalStateException("Можеш да оцениш само събития, за които си бил записан");
        }

        RatingRequest request = new RatingRequest();
        request.setEventId(eventId);
        request.setUserId(userId);
        request.setScore(score);

        try {
            RatingResponse response = ratingClient.createRating(request).getBody();
            logger.info("Rating created successfully for event {} by user {}", eventId, userId);
            return response;
        } catch (FeignException e) {
            if (e.status() == 400) {
                logger.warn("User {} already rated event {}", userId, eventId);
                throw new IllegalStateException("Вече си оценил това събитие");
            }
            logger.error("Error creating rating for event {} by user {}", eventId, userId, e);
            throw new RuntimeException("Грешка при създаване на рейтинг: " + e.getMessage(), e);
        }
    }


    public EventRatingSummaryResponse getRatingsForEvent(UUID eventId) {
        try {
            return ratingClient.getRatingsForEvent(eventId).getBody();
        } catch (Exception e) {
            logger.error("Error fetching ratings for event {}", eventId, e);
            return createEmptyRatingSummary(eventId);
        }
    }

    private EventRatingSummaryResponse createEmptyRatingSummary(UUID eventId) {
        return EventRatingSummaryResponse.builder()
                .eventId(eventId)
                .averageScore(null)
                .totalRatings(0L)
                .ratings(Collections.emptyList())
                .build();
    }

    public boolean hasUserRated(UUID eventId, UUID userId) {
        try {
            Boolean result = ratingClient.hasUserRated(eventId, userId).getBody();
            return result != null && result;
        } catch (Exception e) {
            logger.error("Error checking if user {} has rated event {}", userId, eventId, e);
            return false;
        }
    }

    public Map<UUID, EventRatingSummaryResponse> getRatingsForEvents(List<UUID> eventIds) {
        return eventIds.stream()
                .collect(Collectors.toMap(eventId -> eventId, this::getRatingsForEvent));
    }

    public Map<UUID, Boolean> getHasRatedMapForEvents(List<UUID> eventIds, UUID userId) {
        return eventIds.stream()
                .collect(Collectors.toMap(eventId -> eventId, eventId -> hasUserRated(eventId, userId)));
    }
}

