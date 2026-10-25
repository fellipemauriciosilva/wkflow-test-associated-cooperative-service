package br.coop.pautas.pauta;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LimiteTamanhoCorpoFilter extends OncePerRequestFilter {

    static final int LIMITE_BYTES = 4096;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith("/pautas")) {
            chain.doFilter(request, response);
            return;
        }
        byte[] corpo = request.getInputStream().readNBytes(LIMITE_BYTES + 1);
        if (corpo.length > LIMITE_BYTES) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        chain.doFilter(new CorpoEmMemoria(request, corpo), response);
    }

    private static final class CorpoEmMemoria extends HttpServletRequestWrapper {

        private final byte[] corpo;

        CorpoEmMemoria(HttpServletRequest request, byte[] corpo) {
            super(request);
            this.corpo = corpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(corpo);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return in.read();
                }
            };
        }
    }
}
