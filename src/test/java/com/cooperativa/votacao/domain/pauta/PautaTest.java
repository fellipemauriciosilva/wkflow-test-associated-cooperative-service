package com.cooperativa.votacao.domain.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PautaTest {

    @Test
    @DisplayName("cria pauta valida com titulo e descricao")
    void construtor_comTituloEDescricaoValidos_criaPauta() {
        var pauta = new Pauta("Eleicao de diretoria", "Votacao anual");

        assertThat(pauta.getTitulo()).isEqualTo("Eleicao de diretoria");
        assertThat(pauta.getDescricao()).isEqualTo("Votacao anual");
    }

    @Test
    @DisplayName("cria pauta valida sem descricao")
    void construtor_semDescricao_criaPauta() {
        var pauta = new Pauta("Eleicao de diretoria", null);

        assertThat(pauta.getDescricao()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejeita titulo nulo, vazio ou somente espacos")
    void construtor_comTituloInvalido_lancaExcecao(String titulo) {
        assertThatThrownBy(() -> new Pauta(titulo, "descricao qualquer"))
                .isInstanceOf(PautaInvalidaException.class);
    }

    @Test
    @DisplayName("rejeita titulo acima do tamanho maximo")
    void construtor_comTituloMuitoLongo_lancaExcecao() {
        var tituloLongo = "a".repeat(Pauta.TITULO_TAMANHO_MAXIMO + 1);

        assertThatThrownBy(() -> new Pauta(tituloLongo, null))
                .isInstanceOf(PautaInvalidaException.class);
    }

    @Test
    @DisplayName("aceita titulo no limite exato do tamanho maximo")
    void construtor_comTituloNoLimite_criaPauta() {
        var tituloNoLimite = "a".repeat(Pauta.TITULO_TAMANHO_MAXIMO);

        var pauta = new Pauta(tituloNoLimite, null);

        assertThat(pauta.getTitulo()).hasSize(Pauta.TITULO_TAMANHO_MAXIMO);
    }

    @Test
    @DisplayName("rejeita descricao acima do tamanho maximo")
    void construtor_comDescricaoMuitoLonga_lancaExcecao() {
        var descricaoLonga = "a".repeat(Pauta.DESCRICAO_TAMANHO_MAXIMO + 1);

        assertThatThrownBy(() -> new Pauta("titulo valido", descricaoLonga))
                .isInstanceOf(PautaInvalidaException.class);
    }

    @Test
    @DisplayName("remove espacos nas extremidades do titulo")
    void construtor_comTituloComEspacos_removeEspacosExtremidades() {
        var pauta = new Pauta("  titulo com espacos  ", null);

        assertThat(pauta.getTitulo()).isEqualTo("titulo com espacos");
    }
}
