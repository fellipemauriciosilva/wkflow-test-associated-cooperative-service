package com.cooperativa.votacao.api;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Rejeita requisicoes com body acima do limite antes de o controller terminar de le-lo.
 * Duas camadas: rejeicao imediata se o header Content-Length ja declara um valor acima do
 * limite (evita ate comecar a ler), e um wrapper que conta bytes efetivamente lidos do
 * InputStream e interrompe a leitura ao estourar o limite — cobre requisicoes chunked ou
 * sem Content-Length, onde o header sozinho nao protege nada.
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

        var requisicaoComLimite = new RequisicaoComLimiteDeBytes(httpRequest, TAMANHO_MAXIMO_BYTES);
        try {
            chain.doFilter(requisicaoComLimite, response);
        } catch (CorpoExcedeTamanhoMaximoException ex) {
            responderPayloadTooLarge(httpResponse);
        }
    }

    private void responderPayloadTooLarge(HttpServletResponse response) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.reset();
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":413,\"mensagem\":\"corpo da requisicao excede o tamanho maximo permitido\"}");
    }

    /** Lancada pelo stream ao ultrapassar o limite durante a leitura; nao e IOException de
     * proposito, pois Jackson converte IOException em HttpMessageNotReadableException (400)
     * antes de chegar ao filtro — precisa atravessar essa camada sem ser traduzida. */
    // Publica: precisa ser referenciavel de com.cooperativa.votacao.api.pauta, onde
    // PautaExceptionHandler desembrulha a cadeia de causas de HttpMessageNotReadableException
    // (Jackson embrulha qualquer exececao de leitura do stream nessa, antes do Spring MVC
    // decidir qual @ExceptionHandler chamar — ver PautaExceptionHandler.tratarJsonInvalido).
    public static final class CorpoExcedeTamanhoMaximoException extends RuntimeException {
    }

    private static final class RequisicaoComLimiteDeBytes extends HttpServletRequestWrapper {

        private final long limiteBytes;
        // Cacheado: sem isso, cada chamada a getInputStream() cria um contador zerado sobre
        // o MESMO stream subjacente, permitindo ler o limite inteiro de novo a cada chamada.
        private ServletInputStream streamContado;

        RequisicaoComLimiteDeBytes(HttpServletRequest request, long limiteBytes) {
            super(request);
            this.limiteBytes = limiteBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (streamContado == null) {
                streamContado = new StreamComLimiteDeBytes(super.getInputStream(), limiteBytes);
            }
            return streamContado;
        }

        // HttpServletRequestWrapper.getReader() por padrao delega pro request original,
        // ignorando o wrapper — qualquer consumidor que leia por aqui (em vez de
        // getInputStream()) furava o limite. Forcar via getInputStream() garante a mesma
        // contagem nos dois caminhos.
        @Override
        public java.io.BufferedReader getReader() throws IOException {
            var charset = getCharacterEncoding() != null
                    ? getCharacterEncoding()
                    : java.nio.charset.StandardCharsets.UTF_8.name();
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class StreamComLimiteDeBytes extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long limiteBytes;
        private long bytesLidos = 0;

        StreamComLimiteDeBytes(ServletInputStream delegate, long limiteBytes) {
            this.delegate = delegate;
            this.limiteBytes = limiteBytes;
        }

        @Override
        public int read() throws IOException {
            var lido = delegate.read();
            if (lido != -1) {
                contar(1);
            }
            return lido;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            var lidos = delegate.read(b, off, len);
            if (lidos > 0) {
                contar(lidos);
            }
            return lidos;
        }

        private void contar(int lidos) {
            bytesLidos += lidos;
            if (bytesLidos > limiteBytes) {
                throw new CorpoExcedeTamanhoMaximoException();
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }
}
