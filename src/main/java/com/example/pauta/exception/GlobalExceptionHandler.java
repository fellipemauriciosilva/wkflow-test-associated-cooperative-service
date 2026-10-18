package com.example.pauta.exception;

import com.example.pauta.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation error");
        return ResponseEntity.badRequest().body(ErrorResponse.of(message, HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("Malformed request body received", ex);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("Invalid request body", HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(PautaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PautaNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }
}
