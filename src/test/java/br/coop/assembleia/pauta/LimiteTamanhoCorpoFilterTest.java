package br.coop.assembleia.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LimiteTamanhoCorpoFilterTest {

    private final LimiteTamanhoCorpoFilter filtro = new LimiteTamanhoCorpoFilter();

    @Test
    void corpoChunkedSemContentLengthAcimaDoLimiteRetorna413SemChamarCadeia() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pautas");
        request.setContent(bytes(LimiteTamanhoCorpoFilter.LIMITE_BYTES + 1));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void corpoChunkedNoLimiteExatoPassaParaACadeia() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pautas");
        request.setContent(bytes(LimiteTamanhoCorpoFilter.LIMITE_BYTES));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(request, response, chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void corpoDeUmByteAcimaDoLimiteRetorna413() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pautas");
        request.setContent(bytes(LimiteTamanhoCorpoFilter.LIMITE_BYTES + 1));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(413);
    }

    @Test
    void contentLengthAcimaDoLimiteRetorna413SemLerCorpo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/pautas") {
            @Override
            public long getContentLengthLong() {
                return LimiteTamanhoCorpoFilter.LIMITE_BYTES + 1;
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filtro.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static byte[] bytes(long tamanho) {
        return "a".repeat((int) tamanho).getBytes(StandardCharsets.UTF_8);
    }
}
