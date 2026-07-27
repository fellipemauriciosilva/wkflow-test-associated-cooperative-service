package com.cooperative.domain.exception;

/**
 * Thrown when attempting to create an Associate with a document that already exists.
 */
public class DuplicateDocumentException extends DomainException {

    private static final String MESSAGE_TEMPLATE = "Associate with document '%s' already exists";

    public DuplicateDocumentException(String document) {
        super(String.format(MESSAGE_TEMPLATE, document));
    }
}
