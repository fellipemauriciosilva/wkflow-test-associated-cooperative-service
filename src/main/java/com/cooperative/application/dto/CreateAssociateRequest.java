package com.cooperative.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new associate.
 */
public record CreateAssociateRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, message = "Name must have at least 2 characters")
    String name,
    
    @NotBlank(message = "Document is required")
    String document,
    
    @Email(message = "Email must be valid")
    String email
) {
}
