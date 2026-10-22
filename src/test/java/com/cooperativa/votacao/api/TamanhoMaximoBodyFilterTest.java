package com.cooperativa.votacao.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Testa o filtro isolado, sem o MockMvc, porque MockMvc sempre preenche Content-Length a
 * partir do array de bytes do payload — nao da pra simular, por esse caminho, uma requisicao
 * chunked ou sem esse header (o proprio cenario do bypass, ver H1 do review).
 */
class TamanhoMaximoBodyFilterTest {

    private final TamanhoMaximoBodyFilter filtro = new TamanhoMaximoBodyFilter();

    @Test
    @DisplayName("sem Content-Length confiavel (-1, como em chunked), corta a leitura ao "
            + "ultrapassar o limite e responde 413 em vez de deixar o body fluir sem limite")
    void semContentLengthConfiavel_cortaLeituraAcimaDoLimite_retorna413() throws Exception {
        var corpoAcimaDoLimite = "a".repeat((int) TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES + 1)
                .getBytes(StandardCharsets.UTF_8);
        var request = new RequisicaoSemContentLength(corpoAcimaDoLimite);
        var response = new MockHttpServletResponse();
        var chainQueLeTudo = new ChainQueLeInputStreamAteOFim();

        filtro.doFilter(request, response, chainQueLeTudo);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("\"status\":413");
        // O contador so soma leituras bem-sucedidas, entao nunca pode ultrapassar o limite
        // exato: a exececao e lancada antes de qualquer byte extra ser contabilizado.
        assertThat(chainQueLeTudo.bytesLidosAntesDeFalhar)
                .as("nao deve ler mais que o limite: a leitura precisa ser cortada exatamente nele")
                .isLessThanOrEqualTo(TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES);
    }

    @Test
    @DisplayName("sem Content-Length confiavel (-1), corpo dentro do limite passa normalmente")
    void semContentLengthConfiavel_corpoDentroDoLimite_passaParaOChain() throws Exception {
        var corpoDentroDoLimite = "a".repeat(100).getBytes(StandardCharsets.UTF_8);
        var request = new RequisicaoSemContentLength(corpoDentroDoLimite);
        var response = new MockHttpServletResponse();
        var chainQueLeTudo = new ChainQueLeInputStreamAteOFim();

        filtro.doFilter(request, response, chainQueLeTudo);

        assertThat(chainQueLeTudo.executado).isTrue();
        assertThat(chainQueLeTudo.bytesLidosAntesDeFalhar).isEqualTo(100);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("sem Content-Length confiavel, corpo de exatamente 16 KB passa (boundary do stream, nao so do header)")
    void semContentLengthConfiavel_corpoNoLimiteExato_passaParaOChain() throws Exception {
        var corpoNoLimite = "a".repeat((int) TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES)
                .getBytes(StandardCharsets.UTF_8);
        var request = new RequisicaoSemContentLength(corpoNoLimite);
        var response = new MockHttpServletResponse();
        var chainQueLeTudo = new ChainQueLeInputStreamAteOFim();

        filtro.doFilter(request, response, chainQueLeTudo);

        assertThat(chainQueLeTudo.bytesLidosAntesDeFalhar).isEqualTo(TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("getInputStream() chamado duas vezes reusa o mesmo stream contado, nao reseta o contador (B3)")
    void getInputStream_chamadoDuasVezes_naoResetaContador() throws Exception {
        var corpoAcimaDoLimite = "a".repeat((int) TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES + 1)
                .getBytes(StandardCharsets.UTF_8);
        var request = new RequisicaoSemContentLength(corpoAcimaDoLimite);
        var response = new MockHttpServletResponse();
        var chainQueChamaInputStreamDuasVezes = new ChainQueChamaGetInputStreamDuasVezes();

        filtro.doFilter(request, response, chainQueChamaInputStreamDuasVezes);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chainQueChamaInputStreamDuasVezes.totalBytesLidos)
                .as("a segunda chamada a getInputStream() nao pode reabrir uma cota nova de 16 KB")
                .isLessThanOrEqualTo(TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES);
    }

    @Test
    @DisplayName("getReader() tambem e cortado pelo limite (B2): nao e um caminho que ignora o wrapper")
    void getReader_acimaDoLimite_tambemCorta() throws Exception {
        var corpoAcimaDoLimite = "a".repeat((int) TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES + 1)
                .getBytes(StandardCharsets.UTF_8);
        var request = new RequisicaoSemContentLength(corpoAcimaDoLimite);
        var response = new MockHttpServletResponse();
        var chainQueLeViaReader = new ChainQueLeViaGetReader();

        filtro.doFilter(request, response, chainQueLeViaReader);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chainQueLeViaReader.caracteresLidosAntesDeFalhar)
                .as("getReader() precisa respeitar o mesmo limite de bytes que getInputStream()")
                .isLessThanOrEqualTo(TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES);
    }

    /** Simula o cenario do bypass: getContentLengthLong() retorna -1 mesmo com body presente,
     * como acontece em requisicoes chunked sem header Content-Length. */
    private static final class RequisicaoSemContentLength extends MockHttpServletRequest {

        private final byte[] corpo;

        RequisicaoSemContentLength(byte[] corpo) {
            this.corpo = corpo;
        }

        @Override
        public long getContentLengthLong() {
            return -1;
        }

        @Override
        public int getContentLength() {
            return -1;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new DelegatingServletInputStream(new ByteArrayInputStream(corpo));
        }
    }

    private static final class ChainQueLeInputStreamAteOFim implements FilterChain {

        private long bytesLidosAntesDeFalhar = 0;
        private boolean executado = false;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                throws IOException {
            executado = true;
            var inputStream = request.getInputStream();
            var buffer = new byte[256];
            int lidos;
            while ((lidos = inputStream.read(buffer)) != -1) {
                bytesLidosAntesDeFalhar += lidos;
            }
            response.setContentType("application/json");
            ((jakarta.servlet.http.HttpServletResponse) response).setStatus(200);
        }
    }

    /** Chama getInputStream() duas vezes: prova que o segundo stream reusa a mesma contagem. */
    private static final class ChainQueChamaGetInputStreamDuasVezes implements FilterChain {

        private long totalBytesLidos = 0;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                throws IOException {
            var primeiraChamada = request.getInputStream();
            var buffer = new byte[256];
            int lidos;
            while ((lidos = primeiraChamada.read(buffer)) != -1) {
                totalBytesLidos += lidos;
            }

            var segundaChamada = request.getInputStream();
            while ((lidos = segundaChamada.read(buffer)) != -1) {
                totalBytesLidos += lidos;
            }
        }
    }

    /** Le via getReader() em vez de getInputStream(): prova que o wrapper nao vaza por ali. */
    private static final class ChainQueLeViaGetReader implements FilterChain {

        private long caracteresLidosAntesDeFalhar = 0;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                throws IOException {
            var reader = request.getReader();
            var buffer = new char[256];
            int lidos;
            while ((lidos = reader.read(buffer)) != -1) {
                caracteresLidosAntesDeFalhar += lidos;
            }
        }
    }
}
