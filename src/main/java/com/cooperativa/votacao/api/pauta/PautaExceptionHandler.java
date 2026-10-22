package com.cooperativa.votacao.api.pauta;

import com.cooperativa.votacao.api.pauta.dto.ErroResponse;
import com.cooperativa.votacao.domain.pauta.PautaInvalidaException;
import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PautaController.class)
public class PautaExceptionHandler {

    @ExceptionHandler(PautaInvalidaException.class)
    public ResponseEntity<ErroResponse> tratarPautaInvalida(PautaInvalidaException ex) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(PautaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> tratarPautaNaoEncontrada(PautaNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }
}
