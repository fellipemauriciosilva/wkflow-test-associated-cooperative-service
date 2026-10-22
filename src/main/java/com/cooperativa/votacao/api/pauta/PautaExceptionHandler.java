package com.cooperativa.votacao.api.pauta;

import com.cooperativa.votacao.api.pauta.dto.ErroResponse;
import com.cooperativa.votacao.domain.pauta.PautaInvalidaException;
import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    // HttpMessageNotReadableException (JSON malformado ou 413 disfarcado, ver
    // CorpoExcedeTamanhoMaximoHandler) e tratada SO no handler global: um @RestControllerAdvice
    // escopado tem precedencia sobre o global, entao se esta classe tratasse esse tipo aqui
    // sem o desembrulho de CorpoExcedeTamanhoMaximoException, o 413 nunca chegaria ao global
    // pra rotas de /pautas.

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), "parametro '" + ex.getName() + "' invalido"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarContentTypeNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErroResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "content-type nao suportado"));
    }
}
