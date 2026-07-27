package com.cooperative.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Request DTO for creating a new assembly.
 */
public record CreateAssemblyRequest(
    @NotBlank(message = "Title is required")
    String title,
    
    String description,
    
    Instant scheduledAt
) {
}
