package br.coop.associados.votacao.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;

class LimiteBodyFilterTest {

    private static final long LIMITE = LimiteBodyFilter.TAMANHO_MAXIMO_BYTES;

    @Test
    void entradaAceitaExatamenteOLimiteComReadDeArray() throws IOException {
        LimiteBodyFilter.EntradaComLimite entrada = entrada(new byte[(int) LIMITE]);
        byte[] buffer = new byte[1024];

        int total = 0;
        int n;
        while ((n = entrada.read(buffer, 0, buffer.length)) != -1) {
            total += n;
        }

        assertThat(total).isEqualTo(LIMITE);
    }

    @Test
    void entradaRejeitaUmByteAcimaDoLimiteComReadDeArray() {
        LimiteBodyFilter.EntradaComLimite entrada = entrada(new byte[(int) LIMITE + 1]);
        byte[] buffer = new byte[1024];

        assertThatThrownBy(() -> {
            while (entrada.read(buffer, 0, buffer.length) != -1) {
                // consome até estourar o limite
            }
        }).isInstanceOf(LimiteBodyFilter.LimiteExcedidoException.class);
    }

    @Test
    void entradaAceitaExatamenteOLimiteComReadDeByte() throws IOException {
        LimiteBodyFilter.EntradaComLimite entrada = entrada(new byte[(int) LIMITE]);

        for (int i = 0; i < LIMITE; i++) {
            assertThat(entrada.read()).isNotEqualTo(-1);
        }
        assertThat(entrada.read()).isEqualTo(-1);
    }

    @Test
    void entradaRejeitaUmByteAcimaDoLimiteComReadDeByte() {
        LimiteBodyFilter.EntradaComLimite entrada = entrada(new byte[(int) LIMITE + 1]);

        assertThatThrownBy(() -> {
            for (int i = 0; i < LIMITE + 1; i++) {
                entrada.read();
            }
        }).isInstanceOf(LimiteBodyFilter.LimiteExcedidoException.class);
    }

    @Test
    void requestGetReaderLimitaLeituraAcimaDe16Kb() {
        MockHttpServletRequest mock = new MockHttpServletRequest();
        mock.setContent(new byte[(int) LIMITE + 1]);
        LimiteBodyFilter.RequestComLimite request = new LimiteBodyFilter.RequestComLimite(mock, LIMITE);

        assertThatThrownBy(() -> {
            try (Reader reader = request.getReader()) {
                char[] buffer = new char[1024];
                while (reader.read(buffer) != -1) {
                    // consome até estourar o limite
                }
            }
        }).isInstanceOf(LimiteBodyFilter.LimiteExcedidoException.class);
    }

    @Test
    void requestGetReaderLeCorpoDentroDoLimiteComCharsetDaRequisicao() throws IOException {
        MockHttpServletRequest mock = new MockHttpServletRequest();
        mock.setCharacterEncoding("ISO-8859-1");
        mock.setContent("ção".getBytes(StandardCharsets.ISO_8859_1));
        LimiteBodyFilter.RequestComLimite request = new LimiteBodyFilter.RequestComLimite(mock, LIMITE);

        try (Reader reader = request.getReader()) {
            char[] buffer = new char[16];
            int n = reader.read(buffer);
            assertThat(new String(buffer, 0, n)).isEqualTo("ção");
        }
    }

    private static LimiteBodyFilter.EntradaComLimite entrada(byte[] conteudo) {
        return new LimiteBodyFilter.EntradaComLimite(
                new DelegatingServletInputStream(new ByteArrayInputStream(conteudo)), LIMITE);
    }
}
