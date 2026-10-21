package com.votacao.infrastructure.error;

import com.votacao.domain.pauta.PautaNotFoundException;
import com.votacao.domain.sessao.SessaoJaAbertaException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(PautaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PautaNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(SessaoJaAbertaException.class)
    public ResponseEntity<ErrorResponse> handleSessaoJaAberta(SessaoJaAbertaException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Payload inválido");
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(message, LocalDateTime.now(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
