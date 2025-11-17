package main.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCreateRequest {

    @NotBlank(message = "Въведи име на събитието")
    private String name;

    @NotBlank(message = "Добави описание")
    private String description;

    @NotBlank(message = "Въведи локация/място на събитието")
    private String location;

    private Double latitude;

    private Double longitude;

    private String imageName;

    @NotNull(message = "Избери начална дата")
    @Future(message = "Началото трябва да е в бъдещето")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @NotNull(message = "Избери крайна дата")
    @Future(message = "Краят трябва да е в бъдещето")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    @Positive(message = "Капацитетът трябва да е положително число")
    private Integer capacity;

    @NotNull(message = "Избери категория")
    private UUID categoryId;

   
}

