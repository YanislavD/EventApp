package main.service;

import main.model.Event;
import main.service.EventService;
import main.service.SubscriptionService;
import main.web.client.RatingClient;
import main.web.dto.EventRatingSummaryResponse;
import main.web.dto.RatingRequest;
import main.web.dto.RatingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

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
        if (score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException("Оценката трябва да е между 1 и 5");
        }

        Event event = eventService.getById(eventId);

        if (event.getEndTime() == null || event.getEndTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Можеш да оцениш само минали събития");
        }

        if (!subscriptionService.existsByUserAndEvent(userId, event.getId())) {
            throw new IllegalStateException("Можеш да оцениш само събития, за които си бил записан");
        }

        RatingRequest ratingRequest = new RatingRequest();
        ratingRequest.setEventId(eventId);
        ratingRequest.setUserId(userId);
        ratingRequest.setScore(score);

        try {
            ResponseEntity<RatingResponse> response = ratingClient.createRating(ratingRequest);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Rating created successfully for event {} by user {}", eventId, userId);
                return response.getBody();
            }
            throw new RuntimeException("Failed to create rating");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 400) {
                logger.warn("User {} already rated event {} or invalid request", userId, eventId);
                throw new IllegalStateException("Вече си оценил това събитие");
            }
            logger.error("Error creating rating", e);
            throw new RuntimeException("Грешка при създаване на рейтинг: " + e.getMessage(), e);
        } catch (feign.FeignException e) {
            if (e.status() == 400) {
                logger.warn("User {} already rated event {} or invalid request", userId, eventId);
                throw new IllegalStateException("Вече си оценил това събитие");
            }
            logger.error("Error creating rating", e);
            throw new RuntimeException("Грешка при създаване на рейтинг: " + e.getMessage(), e);
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "";
            if (errorMessage.contains("вече е гласувал") || errorMessage.contains("already rated")) {
                logger.warn("User {} already rated event {}", userId, eventId);
                throw new IllegalStateException("Вече си оценил това събитие");
            }
            logger.error("Error creating rating", e);
            throw new RuntimeException("Грешка при създаване на рейтинг: " + errorMessage, e);
        }
    }


    public EventRatingSummaryResponse getRatingsForEvent(UUID eventId) {
        try {
            ResponseEntity<EventRatingSummaryResponse> response = ratingClient.getRatingsForEvent(eventId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return EventRatingSummaryResponse.builder()
                    .eventId(eventId)
                    .averageScore(null)
                    .totalRatings(0L)
                    .ratings(java.util.Collections.emptyList())
                    .build();
        } catch (Exception e) {
            logger.error("Error fetching ratings for event {}", eventId, e);
            return EventRatingSummaryResponse.builder()
                    .eventId(eventId)
                    .averageScore(null)
                    .totalRatings(0L)
                    .ratings(java.util.Collections.emptyList())
                    .build();
        }
    }

    public boolean hasUserRated(UUID eventId, UUID userId) {
        try {
            ResponseEntity<Boolean> response = ratingClient.hasUserRated(eventId, userId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking if user {} has rated event {}", userId, eventId, e);
            return false;
        }
    }
}

