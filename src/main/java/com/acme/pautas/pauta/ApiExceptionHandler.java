package com.acme.pautas.pauta;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ErroResponse(int status, String erro, List<String> detalhes) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .map(campo -> campo + ": obrigatório e não pode ser vazio")
                .toList();
        return erro(HttpStatus.BAD_REQUEST, "Requisição inválida", detalhes);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErroResponse> requisicaoMalformada() {
        return erro(HttpStatus.BAD_REQUEST, "Requisição malformada", List.of());
    }

    @ExceptionHandler(PautaNaoEncontradaException.class)
    ResponseEntity<ErroResponse> naoEncontrada(PautaNaoEncontradaException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String mensagem, List<String> detalhes) {
        return ResponseEntity.status(status)
                .body(new ErroResponse(status.value(), mensagem, detalhes));
    }
}
