package com.cooperativa.votacao.domain.voto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cooperativa.votacao.domain.pauta.Pauta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class VotoTest {

    private final Pauta pauta = new Pauta("Eleicao de diretoria", null);

    @ParameterizedTest
    @EnumSource(TipoVoto.class)
    @DisplayName("cria voto valido para SIM e NAO")
    void construtor_comAssociadoETipoVotoValidos_criaVoto(TipoVoto tipoVoto) {
        var voto = new Voto(pauta, "associado-1", tipoVoto);

        assertThat(voto.getAssociado()).isEqualTo("associado-1");
        assertThat(voto.getTipoVoto()).isEqualTo(tipoVoto);
        assertThat(voto.getPauta()).isEqualTo(pauta);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejeita associado nulo, vazio ou somente espacos")
    void construtor_comAssociadoInvalido_lancaExcecao(String associado) {
        assertThatThrownBy(() -> new Voto(pauta, associado, TipoVoto.SIM))
                .isInstanceOf(VotoInvalidoException.class)
                .hasMessage("associado e obrigatorio");
    }

    @Test
    @DisplayName("rejeita associado acima do tamanho maximo")
    void construtor_comAssociadoMuitoLongo_lancaExcecao() {
        var associadoLongo = "a".repeat(Voto.ASSOCIADO_TAMANHO_MAXIMO + 1);

        assertThatThrownBy(() -> new Voto(pauta, associadoLongo, TipoVoto.SIM))
                .isInstanceOf(VotoInvalidoException.class);
    }

    @Test
    @DisplayName("aceita associado no limite exato do tamanho maximo")
    void construtor_comAssociadoNoLimite_criaVoto() {
        var associadoNoLimite = "a".repeat(Voto.ASSOCIADO_TAMANHO_MAXIMO);

        var voto = new Voto(pauta, associadoNoLimite, TipoVoto.SIM);

        assertThat(voto.getAssociado()).hasSize(Voto.ASSOCIADO_TAMANHO_MAXIMO);
    }

    @Test
    @DisplayName("aceita associado com 50 emojis (code points), nao conta como 100 UTF-16 units")
    void construtor_comAssociadoComEmojisNoLimite_criaVoto() {
        var associadoComEmojis = "😀".repeat(Voto.ASSOCIADO_TAMANHO_MAXIMO);

        var voto = new Voto(pauta, associadoComEmojis, TipoVoto.SIM);

        assertThat(voto.getAssociado().codePointCount(0, voto.getAssociado().length()))
                .isEqualTo(Voto.ASSOCIADO_TAMANHO_MAXIMO);
    }

    @Test
    @DisplayName("rejeita associado com 51 emojis (code points) acima do limite")
    void construtor_comAssociadoComEmojisAcimaDoLimite_lancaExcecao() {
        var associadoComEmojis = "😀".repeat(Voto.ASSOCIADO_TAMANHO_MAXIMO + 1);

        assertThatThrownBy(() -> new Voto(pauta, associadoComEmojis, TipoVoto.SIM))
                .isInstanceOf(VotoInvalidoException.class);
    }

    @Test
    @DisplayName("rejeita tipo de voto nulo")
    void construtor_comTipoVotoNulo_lancaExcecao() {
        assertThatThrownBy(() -> new Voto(pauta, "associado-1", null))
                .isInstanceOf(VotoInvalidoException.class);
    }

    @Test
    @DisplayName("rejeita pauta nula")
    void construtor_comPautaNula_lancaExcecao() {
        assertThatThrownBy(() -> new Voto(null, "associado-1", TipoVoto.SIM))
                .isInstanceOf(NullPointerException.class);
    }
}
