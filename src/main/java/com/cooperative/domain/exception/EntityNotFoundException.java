package com.cooperative.domain.exception;

/**
 * Thrown when an entity cannot be found by ID.
 */
public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
