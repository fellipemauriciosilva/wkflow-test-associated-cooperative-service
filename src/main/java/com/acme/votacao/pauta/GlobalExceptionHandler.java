package com.acme.votacao.pauta;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, "corpo da requisição ilegível", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(PautaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> handlePautaNaoEncontrada(PautaNaoEncontradaException ex, HttpServletRequest req) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(SessaoJaAbertaException.class)
    public ResponseEntity<ErroResponse> handleSessaoJaAberta(SessaoJaAbertaException ex, HttpServletRequest req) {
        return erro(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String message, HttpServletRequest req) {
        ErroResponse corpo = ErroResponse.of(Instant.now(clock), status, message, req.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
