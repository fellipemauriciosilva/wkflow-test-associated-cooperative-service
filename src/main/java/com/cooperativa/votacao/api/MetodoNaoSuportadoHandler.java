package com.cooperativa.votacao.api;

import com.cooperativa.votacao.api.pauta.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global (nao escopado a um controller), porque HttpRequestMethodNotSupportedException e
 * lancada durante a resolucao de rota, antes de o Spring saber qual HandlerMethod seria
 * chamado — um @RestControllerAdvice com assignableTypes nunca a intercepta.
 */
@RestControllerAdvice
public class MetodoNaoSuportadoHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErroResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), "metodo nao suportado"));
    }
}
