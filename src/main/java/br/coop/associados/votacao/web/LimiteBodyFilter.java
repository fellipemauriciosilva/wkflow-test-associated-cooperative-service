package br.coop.associados.votacao.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LimiteBodyFilter extends OncePerRequestFilter {

    public static final long TAMANHO_MAXIMO_BYTES = 16 * 1024;
    public static final String MENSAGEM_LIMITE = "Corpo da requisição acima do limite de 16 KB";

    private final ObjectMapper objectMapper;

    public LimiteBodyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > TAMANHO_MAXIMO_BYTES) {
            escreverLimiteExcedido(request, response);
            return;
        }
        chain.doFilter(new RequestComLimite(request, TAMANHO_MAXIMO_BYTES), response);
    }

    private void escreverLimiteExcedido(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ApiExceptionHandler.corpo(HttpStatus.PAYLOAD_TOO_LARGE, MENSAGEM_LIMITE, request));
    }

    public static final class LimiteExcedidoException extends IOException {

        public LimiteExcedidoException(long limite) {
            super("Corpo excede o limite de " + limite + " bytes");
        }
    }

    static final class RequestComLimite extends HttpServletRequestWrapper {

        private final long limite;

        RequestComLimite(HttpServletRequest request, long limite) {
            super(request);
            this.limite = limite;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new EntradaComLimite(super.getInputStream(), limite);
        }
    }

    static final class EntradaComLimite extends ServletInputStream {

        private final ServletInputStream entrada;
        private final long limite;
        private long lidos;

        EntradaComLimite(ServletInputStream entrada, long limite) {
            this.entrada = entrada;
            this.limite = limite;
        }

        @Override
        public int read() throws IOException {
            int b = entrada.read();
            if (b != -1) {
                contar(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = entrada.read(buffer, offset, length);
            if (n > 0) {
                contar(n);
            }
            return n;
        }

        private void contar(long n) throws LimiteExcedidoException {
            lidos += n;
            if (lidos > limite) {
                throw new LimiteExcedidoException(limite);
            }
        }

        @Override
        public boolean isFinished() {
            return entrada.isFinished();
        }

        @Override
        public boolean isReady() {
            return entrada.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            entrada.setReadListener(listener);
        }
    }
}
