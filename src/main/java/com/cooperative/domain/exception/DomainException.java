package com.cooperative.domain.exception;

/**
 * Base exception for all domain layer errors.
 * Subclasses represent specific business rule violations.
 */
public abstract class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
