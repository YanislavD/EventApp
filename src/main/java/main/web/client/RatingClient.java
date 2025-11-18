package main.web.client;

import main.web.dto.EventRatingSummaryResponse;
import main.web.dto.RatingRequest;
import main.web.dto.RatingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "rating-service", url = "${rating.service.url}")
public interface RatingClient {

    @PostMapping("/ratings")
    ResponseEntity<RatingResponse> createRating(@RequestBody RatingRequest request);

    @GetMapping("/ratings/event/{eventId}")
    ResponseEntity<EventRatingSummaryResponse> getRatingsForEvent(@PathVariable UUID eventId);

    @GetMapping("/ratings/event/{eventId}/user/{userId}")
    ResponseEntity<Boolean> hasUserRated(@PathVariable("eventId") UUID eventId, @PathVariable("userId") UUID userId);
}

