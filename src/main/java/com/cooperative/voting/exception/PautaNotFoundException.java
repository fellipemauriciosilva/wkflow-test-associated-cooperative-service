package com.cooperative.voting.exception;

/**
 * Thrown when a {@code Pauta} cannot be found for a given identifier.
 */
public class PautaNotFoundException extends RuntimeException {

    public PautaNotFoundException(Long id) {
        super("Pauta with id " + id + " not found");
    }
}
