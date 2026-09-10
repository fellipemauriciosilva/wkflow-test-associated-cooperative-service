package com.example.cooperativevoting.domain.exceptions;

public class DuplicateVoteException extends Exception {
    public DuplicateVoteException(String message) {
        super(message);
    }

    public DuplicateVoteException(String message, Throwable cause) {
        super(message, cause);
    }
}
