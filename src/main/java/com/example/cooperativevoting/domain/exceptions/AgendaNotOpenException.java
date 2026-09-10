package com.example.cooperativevoting.domain.exceptions;

public class AgendaNotOpenException extends Exception {
    public AgendaNotOpenException(String message) {
        super(message);
    }

    public AgendaNotOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
