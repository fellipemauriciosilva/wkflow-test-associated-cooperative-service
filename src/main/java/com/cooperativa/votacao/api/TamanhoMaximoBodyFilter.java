package com.cooperativa.votacao.api;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Rejeita requisicoes com body acima do limite antes de chegar ao controller, evitando
 * desserializar payloads grandes em memoria so para falhar na validacao de dominio.
 * Checa apenas o header Content-Length: nao protege contra chunked transfer encoding sem
 * esse header (fora de escopo desta fatia, ver docs/CONTEXT.md).
 */
@Component
public class TamanhoMaximoBodyFilter implements Filter {

    public static final long TAMANHO_MAXIMO_BYTES = 16 * 1024;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;

        if (httpRequest.getContentLengthLong() > TAMANHO_MAXIMO_BYTES) {
            responderPayloadTooLarge(httpResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    private void responderPayloadTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":413,\"mensagem\":\"corpo da requisicao excede o tamanho maximo permitido\"}");
    }
}
