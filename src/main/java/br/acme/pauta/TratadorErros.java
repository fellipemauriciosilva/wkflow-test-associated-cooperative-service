package br.acme.pauta;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(PautaNaoEncontradaException.class)
    ResponseEntity<ErroPadrao> naoEncontrada(PautaNaoEncontradaException ex, HttpServletRequest request) {
        return responde(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroPadrao> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        FieldError primeiro = ex.getBindingResult().getFieldErrors().get(0);
        String mensagem = primeiro.getField() + ": " + primeiro.getDefaultMessage();
        return responde(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", mensagem, request);
    }

    private ResponseEntity<ErroPadrao> responde(HttpStatus status, String codigo, String mensagem,
                                                HttpServletRequest request) {
        ErroPadrao corpo = new ErroPadrao(status.value(), codigo, mensagem, request.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
