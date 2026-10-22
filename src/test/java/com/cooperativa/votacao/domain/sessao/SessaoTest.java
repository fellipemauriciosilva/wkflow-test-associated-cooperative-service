package com.cooperativa.votacao.domain.sessao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cooperativa.votacao.domain.pauta.Pauta;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SessaoTest {

    private final Pauta pauta = new Pauta("Eleicao de diretoria", null);
    private final Instant abertura = Instant.parse("2026-10-22T10:00:00Z");

    @Test
    @DisplayName("cria sessao com duracao informada e calcula fim a partir da abertura")
    void construtor_comDuracaoInformada_calculaFimCorretamente() {
        var sessao = new Sessao(pauta, 5, abertura);

        assertThat(sessao.getDuracaoMinutos()).isEqualTo(5);
        assertThat(sessao.getAbertura()).isEqualTo(abertura);
        assertThat(sessao.getFim()).isEqualTo(Instant.parse("2026-10-22T10:05:00Z"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("rejeita duracao nula ou menor/igual a zero")
    void validarDuracao_comDuracaoInvalida_lancaExcecao(Integer duracao) {
        assertThatThrownBy(() -> Sessao.validarDuracao(duracao))
                .isInstanceOf(SessaoInvalidaException.class)
                .hasMessage("duracao deve ser maior que zero");
    }

    @Test
    @DisplayName("aceita duracao de exatamente 1 minuto")
    void validarDuracao_comDuracaoMinimaValida_naoLancaExcecao() {
        Sessao.validarDuracao(1);
    }

    @Test
    @DisplayName("aceita duracao no teto maximo e rejeita teto + 1")
    void validarDuracao_comDuracaoNoTetoERejeitandoAcimaDoTeto() {
        Sessao.validarDuracao(Sessao.DURACAO_MAXIMA_MINUTOS);

        assertThatThrownBy(() -> Sessao.validarDuracao(Sessao.DURACAO_MAXIMA_MINUTOS + 1))
                .isInstanceOf(SessaoInvalidaException.class)
                .hasMessage("duracao deve ser no maximo " + Sessao.DURACAO_MAXIMA_MINUTOS + " minutos");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("construtor rejeita duracao invalida (nao so validarDuracao isolado)")
    void construtor_comDuracaoInvalida_lancaExcecao(int duracaoInvalida) {
        assertThatThrownBy(() -> new Sessao(pauta, duracaoInvalida, abertura))
                .isInstanceOf(SessaoInvalidaException.class)
                .hasMessage("duracao deve ser maior que zero");
    }

    @Test
    @DisplayName("construtor rejeita abertura nula")
    void construtor_comAberturaNula_lancaExcecao() {
        assertThatThrownBy(() -> new Sessao(pauta, 5, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("abertura e obrigatoria");
    }

    @Test
    @DisplayName("sessao esta aberta quando instante atual e anterior ao fim")
    void estaAberta_comInstanteAntesDoFim_retornaTrue() {
        var sessao = new Sessao(pauta, 10, abertura);

        assertThat(sessao.estaAberta(abertura.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("sessao nao esta aberta quando instante atual e exatamente o fim")
    void estaAberta_comInstanteNoFim_retornaFalse() {
        var sessao = new Sessao(pauta, 10, abertura);

        assertThat(sessao.estaAberta(sessao.getFim())).isFalse();
    }

    @Test
    @DisplayName("sessao nao esta aberta quando instante atual e posterior ao fim")
    void estaAberta_comInstanteDepoisDoFim_retornaFalse() {
        var sessao = new Sessao(pauta, 10, abertura);

        assertThat(sessao.estaAberta(sessao.getFim().plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("rejeita pauta nula")
    void construtor_comPautaNula_lancaExcecao() {
        assertThatThrownBy(() -> new Sessao(null, 5, abertura))
                .isInstanceOf(NullPointerException.class);
    }
}
