package br.acme.pauta;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratadorErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorErros.class);

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroPadrao> corpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return responde(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "corpo: JSON invalido", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroPadrao> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return responde(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getName() + ": formato invalido", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroPadrao> metodoNaoPermitido(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return responde(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErroPadrao> tipoNaoSuportado(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return responde(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroPadrao> inesperado(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse resposta) {
            HttpStatus status = HttpStatus.valueOf(resposta.getStatusCode().value());
            return responde(status, status.name(), ex.getMessage(), request);
        }
        log.error("Erro nao tratado em {}", request.getRequestURI(), ex);
        return responde(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erro interno do servidor", request);
    }

    private ResponseEntity<ErroPadrao> responde(HttpStatus status, String codigo, String mensagem,
                                                HttpServletRequest request) {
        ErroPadrao corpo = new ErroPadrao(status.value(), codigo, mensagem, request.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
