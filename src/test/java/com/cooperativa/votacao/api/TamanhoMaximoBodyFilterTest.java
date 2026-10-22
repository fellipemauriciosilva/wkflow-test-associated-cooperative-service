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
        assertThat(chainQueLeTudo.bytesLidosAntesDeFalhar)
                .as("nao deve ler o corpo inteiro: a leitura precisa ser cortada no limite")
                .isLessThanOrEqualTo(TamanhoMaximoBodyFilter.TAMANHO_MAXIMO_BYTES + 1);
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
}
