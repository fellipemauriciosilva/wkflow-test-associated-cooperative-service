package com.cooperative.domain.exception;

/**
 * Thrown when an Assembly entity violates business rules.
 */
public class InvalidAssemblyException extends DomainException {

    public InvalidAssemblyException(String message) {
        super(message);
    }

    public InvalidAssemblyException(String message, Throwable cause) {
        super(message, cause);
    }
}
