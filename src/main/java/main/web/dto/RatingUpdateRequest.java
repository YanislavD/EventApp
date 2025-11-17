package main.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RatingUpdateRequest {

    @Min(value = 1, message = "Оценката трябва да е между 1 и 5")
    @Max(value = 5, message = "Оценката трябва да е между 1 и 5")
    private Integer score;

    @Size(max = 500, message = "Коментарът не може да бъде по-дълъг от 500 символа")
    private String comment;
}

