package main.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @Size(min = 6, message = "Username must to be at least 6 character")
    private String username;

    @NotBlank
    @Email(message = "Invalid email")
    private String email;

    @Size(min = 6, message = "Password must to be at leat 6 character")
    private String password;
}
