package com.acme.pautas.api;

import com.acme.pautas.domain.PautaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PautaController.class)
public class PautaExceptionHandler {

    @ExceptionHandler(PautaNaoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void naoEncontrada() {
    }
}
