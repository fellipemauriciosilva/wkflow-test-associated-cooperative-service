package com.votacao.application.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.votacao.domain.pauta.Pauta;
import com.votacao.domain.pauta.PautaNotFoundException;
import com.votacao.domain.pauta.PautaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PautaServiceTest {

    @Mock
    private PautaRepository pautaRepository;

    private PautaService sut;

    @Test
    @DisplayName("criar deve salvar e retornar a pauta")
    void criar_deveSalvarERetornarPauta() {
        sut = new PautaService(pautaRepository);
        Pauta salva = new Pauta("Título", "Descrição");
        when(pautaRepository.save(any(Pauta.class))).thenReturn(salva);

        Pauta resultado = sut.criar("Título", "Descrição");

        assertThat(resultado.getTitulo()).isEqualTo("Título");
        assertThat(resultado.getDescricao()).isEqualTo("Descrição");
        verify(pautaRepository).save(any(Pauta.class));
    }

    @Test
    @DisplayName("buscarPorId deve retornar a pauta quando existir")
    void buscarPorId_quandoExiste_retornaPauta() {
        sut = new PautaService(pautaRepository);
        Pauta pauta = new Pauta("Título", "Descrição");
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));

        Pauta resultado = sut.buscarPorId(1L);

        assertThat(resultado).isSameAs(pauta);
    }

    @Test
    @DisplayName("buscarPorId deve lançar PautaNotFoundException quando não existir")
    void buscarPorId_quandoNaoExiste_lancaExcecao() {
        sut = new PautaService(pautaRepository);
        when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.buscarPorId(99L))
                .isInstanceOf(PautaNotFoundException.class);
    }
}
