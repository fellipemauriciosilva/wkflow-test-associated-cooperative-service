package com.cooperative.associated.domain.exception;

/**
 * Thrown when Associate domain invariants are violated (e.g. missing name,
 * invalid email format). Pure domain exception - no framework dependencies.
 */
public class InvalidAssociateDataException extends RuntimeException {

    public InvalidAssociateDataException(String message) {
        super(message);
    }
}
