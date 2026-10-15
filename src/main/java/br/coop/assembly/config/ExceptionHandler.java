package br.coop.assembly.config;

import br.coop.assembly.exception.DuracaoInvalidaException;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.PautaValidationException;
import br.coop.assembly.exception.SessaoJaAbertaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ExceptionHandler {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @org.springframework.web.bind.annotation.ExceptionHandler(PautaNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePautaNotFound(
            PautaNotFoundException ex,
            WebRequest request) {
        log.warn("Pauta not found: {}", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(PautaValidationException.class)
    public ResponseEntity<Map<String, Object>> handlePautaValidation(
            PautaValidationException ex,
            WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(SessaoJaAbertaException.class)
    public ResponseEntity<Map<String, Object>> handleSessaoJaAberta(
            SessaoJaAbertaException ex,
            WebRequest request) {
        log.warn("Session already open: {}", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(
                ex.getCode(),
                "Sessao ja aberta para esta pauta",
                request
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(DuracaoInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleDuracaoInvalida(
            DuracaoInvalidaException ex,
            WebRequest request) {
        log.warn("Invalid duration: {}", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(
                ex.getCode(),
                ex.getMessage(),
                request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());
        Map<String, Object> response = buildErrorResponse(
                "VALIDATION_ERROR",
                "Validation failed",
                request
        );

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage(),
                        "rejectedValue", error.getRejectedValue() != null ? error.getRejectedValue() : ""
                ))
                .toList();

        response.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected error: ", ex);
        Map<String, Object> response = buildErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private Map<String, Object> buildErrorResponse(String code, String message, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        response.put("path", request.getDescription(false).replace("uri=", ""));
        return response;
    }
}
