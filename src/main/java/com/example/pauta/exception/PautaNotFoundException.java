package com.example.pauta.exception;

public class PautaNotFoundException extends RuntimeException {

    public PautaNotFoundException(Long id) {
        super("Pauta not found with id: " + id);
    }
}
