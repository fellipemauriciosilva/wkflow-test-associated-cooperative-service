package pautas;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroResponse.CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErroResponse.CampoErro(e.getField(), e.getDefaultMessage()))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", request, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", request, List.of());
    }

    @ExceptionHandler(PautaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> naoEncontrada(PautaNaoEncontradaException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    private ResponseEntity<ErroResponse> resposta(
            HttpStatus status, String message, HttpServletRequest request, List<ErroResponse.CampoErro> campos) {
        ErroResponse corpo = new ErroResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
