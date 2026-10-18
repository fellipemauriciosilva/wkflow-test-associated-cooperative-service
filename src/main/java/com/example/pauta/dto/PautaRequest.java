package com.example.pauta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank(message = "title must not be blank")
        @Size(min = 1, max = 255, message = "title must be between 1 and 255 characters")
        String title) {
}
