package br.coop.associados.votacao.web;

import br.coop.associados.votacao.pauta.PautaNaoEncontradaException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PautaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> naoEncontrada(PautaNaoEncontradaException ex, HttpServletRequest request) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String campos = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return erro(HttpStatus.BAD_REQUEST, campos, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (causadoPorLimite(ex)) {
            return erro(HttpStatus.PAYLOAD_TOO_LARGE, LimiteBodyFilter.MENSAGEM_LIMITE, request);
        }
        return erro(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "Parâmetro 'id' inválido", request);
    }

    private static boolean causadoPorLimite(Throwable ex) {
        for (Throwable atual = ex; atual != null; atual = atual.getCause()) {
            if (atual instanceof LimiteBodyFilter.LimiteExcedidoException) {
                return true;
            }
        }
        return false;
    }

    public static ResponseEntity<ErroResponse> erro(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(corpo(status, message, request));
    }

    public static ErroResponse corpo(HttpStatus status, String message, HttpServletRequest request) {
        return new ErroResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
    }

    public record ErroResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path) {
    }
}
