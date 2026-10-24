package com.acme.pautas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LimiteTamanhoCorpoFilter extends OncePerRequestFilter {

    static final int LIMITE_BYTES = 64 * 1024;

    private final ObjectMapper mapper;

    public LimiteTamanhoCorpoFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] corpo = lerAteLimite(request.getInputStream());
        if (corpo == null) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), new ErroResponse(
                    HttpStatus.PAYLOAD_TOO_LARGE.value(),
                    "PAYLOAD_MUITO_GRANDE",
                    "corpo excede " + LIMITE_BYTES + " bytes",
                    request.getRequestURI()));
            return;
        }
        chain.doFilter(new CorpoBufferizado(request, corpo), response);
    }

    private static byte[] lerAteLimite(InputStream entrada) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        byte[] bloco = new byte[8192];
        int lidos;
        while ((lidos = entrada.read(bloco)) != -1) {
            if (saida.size() + lidos > LIMITE_BYTES) {
                return null;
            }
            saida.write(bloco, 0, lidos);
        }
        return saida.toByteArray();
    }

    private static final class CorpoBufferizado extends HttpServletRequestWrapper {

        private final byte[] corpo;

        CorpoBufferizado(HttpServletRequest request, byte[] corpo) {
            super(request);
            this.corpo = corpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream entrada = new ByteArrayInputStream(corpo);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return entrada.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener listener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return entrada.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
