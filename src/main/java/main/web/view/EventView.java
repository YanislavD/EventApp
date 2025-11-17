package main.web.view;

import java.time.LocalDateTime;
import java.util.UUID;

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

    public EventView(UUID id,
                     String name,
                     String description,
                     String location,
                     Double latitude,
                     Double longitude,
                     String imageName,
                     LocalDateTime startTime,
                     LocalDateTime endTime,
                     Integer capacity,
                     long remainingCapacity,
                     long registeredCount,
                     boolean full,
                     String categoryName,
                     UUID creatorId,
                     String creatorName,
                     boolean subscribed,
                     String ticketCode) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageName = imageName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.remainingCapacity = remainingCapacity;
        this.registeredCount = registeredCount;
        this.full = full;
        this.categoryName = categoryName;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.subscribed = subscribed;
        this.ticketCode = ticketCode;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getImageName() {
        return imageName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public long getRemainingCapacity() {
        return remainingCapacity;
    }

    public long getRegisteredCount() {
        return registeredCount;
    }

    public boolean isFull() {
        return full;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public String getTicketCode() {
        return ticketCode;
    }
}

