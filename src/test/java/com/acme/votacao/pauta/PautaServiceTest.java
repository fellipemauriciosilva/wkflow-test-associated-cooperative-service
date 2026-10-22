package com.acme.votacao.pauta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PautaServiceTest {

    @Mock
    private PautaRepository pautaRepository;

    private PautaService sut;

    @BeforeEach
    void setUp() {
        sut = new PautaService(pautaRepository);
    }

    @Test
    @DisplayName("deve criar pauta e persistir via repositório")
    void criar_persisteEPautaRetornada() {
        var request = new PautaRequest("Reforma do estatuto", "Discussão sobre regras internas");
        // valores diferentes do request: garante que as asserções abaixo não coincidem por acaso
        var pautaSalva = new Pauta("Reforma do estatuto (persistida)", "Discussão sobre regras internas (persistida)");
        when(pautaRepository.save(org.mockito.ArgumentMatchers.any(Pauta.class))).thenReturn(pautaSalva);

        var resultado = sut.criar(request);

        ArgumentCaptor<Pauta> captor = ArgumentCaptor.forClass(Pauta.class);
        verify(pautaRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Reforma do estatuto");
        assertThat(captor.getValue().getDescricao()).isEqualTo("Discussão sobre regras internas");

        assertThat(resultado).isSameAs(pautaSalva);
    }

    @Test
    @DisplayName("deve retornar pauta quando id existe")
    void buscarPorId_quandoExiste_retornaPauta() {
        var pauta = new Pauta("Eleição diretoria", null);
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));

        var resultado = sut.buscarPorId(1L);

        assertThat(resultado.getTitulo()).isEqualTo("Eleição diretoria");
    }

    @Test
    @DisplayName("deve lançar exceção quando id não existe")
    void buscarPorId_quandoNaoExiste_lancaExcecao() {
        when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.buscarPorId(99L))
                .isInstanceOf(PautaNaoEncontradaException.class);
    }
}
