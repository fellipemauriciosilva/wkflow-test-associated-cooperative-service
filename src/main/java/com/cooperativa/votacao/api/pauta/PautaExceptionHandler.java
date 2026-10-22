package com.cooperativa.votacao.api.pauta;

import com.cooperativa.votacao.api.TamanhoMaximoBodyFilter;
import com.cooperativa.votacao.api.pauta.dto.ErroResponse;
import com.cooperativa.votacao.domain.pauta.PautaInvalidaException;
import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        // Jackson embrulha qualquer exceção lançada durante a leitura do InputStream (via
        // RequestResponseBodyMethodProcessor) em JsonMappingException -> aqui. A excecao do
        // limite de body (lancada dentro do read() do stream contado) chega disfarcada de
        // "JSON invalido" se nao desembrulharmos a cadeia de causas primeiro.
        if (causaEhExcederLimiteDeBody(ex)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(new ErroResponse(HttpStatus.PAYLOAD_TOO_LARGE.value(),
                            "corpo da requisicao excede o tamanho maximo permitido"));
        }
        return ResponseEntity.badRequest()
                .body(new ErroResponse(HttpStatus.BAD_REQUEST.value(), "corpo da requisicao invalido ou ausente"));
    }

    private static boolean causaEhExcederLimiteDeBody(Throwable ex) {
        for (var causa = ex; causa != null; causa = causa.getCause()) {
            if (causa instanceof TamanhoMaximoBodyFilter.CorpoExcedeTamanhoMaximoException) {
                return true;
            }
        }
        return false;
    }

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
