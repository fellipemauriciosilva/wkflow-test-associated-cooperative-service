package com.cooperative.domain.validation;

import com.cooperative.domain.exception.InvalidAssemblyException;
import java.time.Instant;

/**
 * Validation logic for Assembly entity.
 */
public class AssemblyValidator {

    private AssemblyValidator() {
        // Utility class, no instantiation
    }

    /**
     * Validates assembly fields.
     *
     * @param title the assembly title (required)
     * @param description the assembly description (optional)
     * @param scheduledAt the assembly scheduled time (optional)
     * @throws InvalidAssemblyException if validation fails
     */
    public static void validate(String title, String description, Instant scheduledAt) {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidAssemblyException(
                "Assembly title is required");
        }
    }
}
