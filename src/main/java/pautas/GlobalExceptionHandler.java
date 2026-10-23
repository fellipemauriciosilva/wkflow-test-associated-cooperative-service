package pautas;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
        List<ErroResponse.CampoErro> campos = ex.getCause() instanceof InvalidFormatException ife
                && !ife.getPath().isEmpty()
                ? List.of(new ErroResponse.CampoErro(ife.getPath().get(0).getFieldName(), "tipo inválido"))
                : List.of();
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", request, campos);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName(), request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tipoNaoSuportado(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de mídia não suportado", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoPermitido(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", request, List.of());
    }

    @ExceptionHandler(PautaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> naoEncontrada(PautaNaoEncontradaException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(SessaoJaExisteException.class)
    public ResponseEntity<ErroResponse> sessaoExistente(SessaoJaExisteException ex, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
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
