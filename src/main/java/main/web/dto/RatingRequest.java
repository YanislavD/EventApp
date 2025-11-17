package main.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RatingRequest {

    @NotNull(message = "Event ID е задължителен")
    private UUID eventId;

    @NotNull(message = "User ID е задължителен")
    private UUID userId;

    @NotNull(message = "Оценката е задължителна")
    @Min(value = 1, message = "Оценката трябва да е между 1 и 5")
    @Max(value = 5, message = "Оценката трябва да е между 1 и 5")
    private Integer score;
}

