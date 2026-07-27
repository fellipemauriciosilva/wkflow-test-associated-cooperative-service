package com.cooperative.domain.entity;

import com.cooperative.domain.exception.InvalidAssemblyException;
import com.cooperative.domain.validation.AssemblyValidator;
import java.time.Instant;

/**
 * Immutable Assembly domain entity.
 * Represents a voting assembly.
 */
public record Assembly(
    Long id,
    String title,
    String description,
    Instant scheduledAt,
    Instant createdAt
) {

    /**
     * Compact constructor enforces invariants on all fields.
     */
    public Assembly {
        if (id != null && id <= 0) {
            throw new InvalidAssemblyException("Invalid assembly ID: must be positive");
        }
        if (title != null && title.isBlank()) {
            throw new InvalidAssemblyException("Assembly title cannot be blank");
        }
    }

    /**
     * Factory method for creating new assemblies (before persistence).
     * Validates required fields and assigns current timestamp.
     *
     * @param title the assembly title (required)
     * @param description the assembly description (optional)
     * @param scheduledAt the assembly scheduled time (optional, ISO 8601 format)
     * @return newly created Assembly with null ID (assigned by database)
     * @throws InvalidAssemblyException if validation fails
     */
    public static Assembly create(String title, String description, Instant scheduledAt) {
        AssemblyValidator.validate(title, description, scheduledAt);
        return new Assembly(
            null,  // ID assigned by database
            title.trim(),
            description != null ? description.trim() : null,
            scheduledAt,
            Instant.now()
        );
    }
}
