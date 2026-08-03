package com.cooperative.associated.application.exception;

public class DuplicateDocumentException extends RuntimeException {

    public DuplicateDocumentException(String document) {
        super("document already registered: " + document);
    }
}



