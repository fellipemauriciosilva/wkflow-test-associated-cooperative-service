package com.acme.votacao.pauta;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Estende {@link ResponseEntityExceptionHandler} em vez de usar só {@code @ExceptionHandler(Exception.class)}
 * avulso: o catch-all genérico capturava exceções do próprio Spring MVC (parâmetro obrigatório ausente,
 * Accept não aceitável, etc.) antes do resolver padrão do framework tratá-las, convertendo 4xx em 500.
 * Estender a classe base e sobrescrever {@code handleExceptionInternal} preserva o status que o Spring MVC
 * já calculou para essas exceções, só trocando o corpo pelo {@link ErroResponse} padronizado da API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;
    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(Clock clock, ObjectMapper objectMapper) {
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    /**
     * Escreve o corpo de erro diretamente na resposta (bypassando os message converters do Spring MVC)
     * em vez de retornar um {@code ResponseEntity}: para exceções como
     * {@code HttpMediaTypeNotAcceptableException} ("Accept" do cliente não aceita nada que o servidor
     * produza), deixar o fluxo normal de negociação de conteúdo decidir como serializar o próprio corpo
     * de erro reproduziria o mesmo 406 na escrita da resposta de erro.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                               HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String mensagem = switch (ex) {
            case org.springframework.web.bind.MethodArgumentNotValidException manv -> mensagemDeValidacao(manv);
            case org.springframework.beans.TypeMismatchException tme -> "parâmetro com tipo inválido";
            default -> status.is5xxServerError() ? "erro interno inesperado" : status.getReasonPhrase();
        };
        if (status.is5xxServerError()) {
            log.error("Erro não tratado ao processar requisição: {}", caminho(request), ex);
        }
        escreverErro(status, mensagem, caminho(request), request);
        return null;
    }

    private void escreverErro(HttpStatus status, String message, String path, WebRequest request) {
        if (!(request instanceof ServletWebRequest servletWebRequest)) {
            return;
        }
        try {
            var response = servletWebRequest.getResponse();
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), erro(status, message, path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ExceptionHandler(PautaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> handlePautaNaoEncontrada(PautaNaoEncontradaException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(SessaoJaAbertaException.class)
    public ResponseEntity<ErroResponse> handleSessaoJaAberta(SessaoJaAbertaException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex, HttpServletRequest req) {
        log.error("Erro não tratado ao processar {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(erro(HttpStatus.INTERNAL_SERVER_ERROR, "erro interno inesperado", req.getRequestURI()));
    }

    private String mensagemDeValidacao(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return mensagem.isBlank() ? "requisição inválida" : mensagem;
    }

    private String caminho(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false).replaceFirst("^uri=", "");
    }

    private ErroResponse erro(HttpStatus status, String message, String path) {
        return ErroResponse.of(Instant.now(clock), status, message, path);
    }
}
