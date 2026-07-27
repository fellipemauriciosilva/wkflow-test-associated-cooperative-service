package com.cooperative.domain.exception;

/**
 * Thrown when an Associate entity violates business rules.
 */
public class InvalidAssociateException extends DomainException {

    public InvalidAssociateException(String message) {
        super(message);
    }

    public InvalidAssociateException(String message, Throwable cause) {
        super(message, cause);
    }
}
