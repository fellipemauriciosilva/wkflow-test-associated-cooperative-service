package com.cooperative.domain.entity;

import com.cooperative.domain.exception.InvalidAssociateException;
import com.cooperative.domain.validation.AssociateValidator;
import java.time.Instant;

/**
 * Immutable Associate domain entity.
 * Represents a member of the cooperative.
 */
public record Associate(
    Long id,
    String name,
    String document,
    String email,
    Instant createdAt
) {

    /**
     * Compact constructor enforces invariants on all fields.
     */
    public Associate {
        if (id != null && id <= 0) {
            throw new InvalidAssociateException("Invalid associate ID: must be positive");
        }
        if (name != null && name.isBlank()) {
            throw new InvalidAssociateException("Associate name cannot be blank");
        }
        if (document != null && document.isBlank()) {
            throw new InvalidAssociateException("Associate document cannot be blank");
        }
    }

    /**
     * Factory method for creating new associates (before persistence).
     * Validates all fields and assigns current timestamp.
     *
     * @param name the associate name (required, min 2 characters)
     * @param document the associate document (required, unique)
     * @param email the associate email (optional, valid format if provided)
     * @return newly created Associate with null ID (assigned by database)
     * @throws InvalidAssociateException if validation fails
     */
    public static Associate create(String name, String document, String email) {
        AssociateValidator.validate(name, document, email);
        return new Associate(
            null,  // ID assigned by database
            name.trim(),
            document.trim(),
            email != null ? email.trim() : null,
            Instant.now()
        );
    }
}
