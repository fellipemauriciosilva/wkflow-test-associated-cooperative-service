package com.example.pauta.dto;

import jakarta.validation.constraints.NotBlank;

public record PautaRequest(@NotBlank(message = "title must not be blank") String title) {
}
