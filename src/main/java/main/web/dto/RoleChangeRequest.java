package main.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleChangeRequest {

    @NotBlank(message = "Ролята е задължителна")
    private String role;
}

