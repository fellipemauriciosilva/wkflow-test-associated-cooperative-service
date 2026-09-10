package com.example.cooperativevoting.domain.exceptions;

public class DuplicateAssociateException extends Exception {
    public DuplicateAssociateException(String message) {
        super(message);
    }

    public DuplicateAssociateException(String message, Throwable cause) {
        super(message, cause);
    }
}
