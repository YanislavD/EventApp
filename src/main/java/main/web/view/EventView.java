package main.web.view;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class EventView {

    private final UUID id;
    private final String name;
    private final String description;
    private final String location;
    private final Double latitude;
    private final Double longitude;
    private final String imageName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Integer capacity;
    private final long remainingCapacity;
    private final long registeredCount;
    private final boolean full;
    private final String categoryName;
    private final UUID creatorId;
    private final String creatorName;
    private final boolean subscribed;
    private final String ticketCode;

}

