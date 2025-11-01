package main.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryCreateRequest {

    @NotBlank(message = "Въведи име на категорията")
    @Size(max = 64, message = "Името трябва да е до 64 символа")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

