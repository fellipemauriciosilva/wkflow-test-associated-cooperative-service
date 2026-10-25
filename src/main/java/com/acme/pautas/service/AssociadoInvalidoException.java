package com.acme.pautas.service;

public class AssociadoInvalidoException extends RuntimeException {

    public AssociadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
