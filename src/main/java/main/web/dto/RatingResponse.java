package main.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {

    private UUID id;
    private UUID eventId;
    private UUID userId;
    private Integer score;
    private String comment;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}

