package com.acme.pautas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PautaTest {

    @Test
    void criaComTituloEDescricao() {
        Pauta pauta = Pauta.criar("Título", "Descrição");

        assertThat(pauta.getTitulo()).isEqualTo("Título");
        assertThat(pauta.getDescricao()).isEqualTo("Descrição");
    }

    @Test
    void aceitaDescricaoNula() {
        assertThat(Pauta.criar("Título", null).getDescricao()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejeitaTituloNuloVazioOuEmBranco(String titulo) {
        assertThatThrownBy(() -> Pauta.criar(titulo, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
