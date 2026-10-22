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
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
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

    @Test
    @DisplayName("remove em space (U+2003) nas extremidades do titulo, pois e whitespace Unicode")
    void construtor_comTituloComEmSpace_removeEspacosExtremidades() {
        var pauta = new Pauta("\u2003titulo com em space\u2003", null);

        assertThat(pauta.getTitulo()).isEqualTo("titulo com em space");
    }

    @Test
    @DisplayName("rejeita titulo somente com em space (U+2003), pois e whitespace Unicode")
    void construtor_comTituloSoEmSpace_lancaExcecao() {
        assertThatThrownBy(() -> new Pauta("\u2003\u2003", null))
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
    }

    @Test
    @DisplayName("rejeita titulo somente com NBSP (U+00A0): nao e whitespace para "
            + "isBlank/strip, mas e Zs (SPACE_SEPARATOR), tratado como 'sem caractere visivel'")
    void construtor_comTituloSoNbsp_lancaExcecao() {
        assertThatThrownBy(() -> new Pauta("\u00A0", null))
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
    }

    @Test
    @DisplayName("rejeita titulo somente com zero-width space (U+200B): categoria Cf "
            + "(format), tratado como 'sem caractere visivel'")
    void construtor_comTituloSoZeroWidthSpace_lancaExcecao() {
        assertThatThrownBy(() -> new Pauta("\u200B", null))
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
    }

    @Test
    @DisplayName("rejeita titulo so com BOM/zero-width no-break space (U+FEFF)")
    void construtor_comTituloSoBom_lancaExcecao() {
        assertThatThrownBy(() -> new Pauta("\uFEFF", null))
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
    }

    @Test
    @DisplayName("rejeita titulo so com line separator (U+2028): categoria Zl")
    void construtor_comTituloSoLineSeparator_lancaExcecao() {
        assertThatThrownBy(() -> new Pauta("\u2028", null))
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
    }

    @Test
    @DisplayName("rejeita titulo so com paragraph separator (U+2029): categoria Zp")
    void construtor_comTituloSoParagraphSeparator_lancaExcecao() {
        assertThatThrownBy(() -> new Pauta("\u2029", null))
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
    }

    @Test
    @DisplayName("rejeita titulo so com caractere de controle (U+0085, NEL): categoria Cc")
    void construtor_comTituloSoControle_lancaExcecao() {
        assertThatThrownBy(() -> new Pauta("\u0085", null))
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
    }

    @Test
    @DisplayName("rejeita titulo so com code point nao atribuido (U+0378): categoria UNASSIGNED")
    void construtor_comTituloSoNaoAtribuido_lancaExcecao() {
        assertThatThrownBy(() -> new Pauta("\u0378", null))
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
    }

    @Test
    @DisplayName("rejeita titulo so com surrogate solitario (U+D800): categoria SURROGATE")
    void construtor_comTituloSoSurrogateSolitario_lancaExcecao() {
        assertThatThrownBy(() -> new Pauta("\uD800", null))
                .isInstanceOf(PautaInvalidaException.class)
                .hasMessage("titulo e obrigatorio");
    }

    @Test
    @DisplayName("aceita NBSP e zero-width space quando ha pelo menos um caractere visivel no titulo")
    void construtor_comTituloVisivelMaisInvisiveis_criaPauta() {
        var pauta = new Pauta("titulo\u00A0com\u200Binvisiveis", null);

        assertThat(pauta.getTitulo()).isEqualTo("titulo\u00A0com\u200Binvisiveis");
    }

    @Test
    @DisplayName("aceita titulo com 200 emojis (code points), nao conta como 400 UTF-16 units")
    void construtor_comTituloComEmojisNoLimite_criaPauta() {
        var tituloComEmojis = "😀".repeat(Pauta.TITULO_TAMANHO_MAXIMO);

        var pauta = new Pauta(tituloComEmojis, null);

        assertThat(pauta.getTitulo().codePointCount(0, pauta.getTitulo().length()))
                .isEqualTo(Pauta.TITULO_TAMANHO_MAXIMO);
    }

    @Test
    @DisplayName("rejeita titulo com 201 emojis (code points) acima do limite")
    void construtor_comTituloComEmojisAcimaDoLimite_lancaExcecao() {
        var tituloComEmojis = "😀".repeat(Pauta.TITULO_TAMANHO_MAXIMO + 1);

        assertThatThrownBy(() -> new Pauta(tituloComEmojis, null))
                .isInstanceOf(PautaInvalidaException.class);
    }

    @Test
    @DisplayName("aceita descricao no limite exato do tamanho maximo")
    void construtor_comDescricaoNoLimite_criaPauta() {
        var descricaoNoLimite = "a".repeat(Pauta.DESCRICAO_TAMANHO_MAXIMO);

        var pauta = new Pauta("titulo valido", descricaoNoLimite);

        assertThat(pauta.getDescricao()).hasSize(Pauta.DESCRICAO_TAMANHO_MAXIMO);
    }
}
