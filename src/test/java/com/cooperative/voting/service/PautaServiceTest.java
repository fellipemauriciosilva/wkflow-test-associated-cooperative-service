package com.cooperative.voting.service;

import com.cooperative.voting.entity.Pauta;
import com.cooperative.voting.exception.PautaNotFoundException;
import com.cooperative.voting.repository.PautaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PautaServiceTest {

    @Mock
    private PautaRepository pautaRepository;

    @InjectMocks
    private PautaService pautaService;

    @Test
    void testCreatePauta_Success() {
        LocalDateTime date = LocalDateTime.of(2026, 2, 15, 10, 0);
        Pauta saved = Pauta.builder()
                .id(1L)
                .title("Pauta 1")
                .description("Descricao")
                .date(date)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(pautaRepository.save(any(Pauta.class))).thenReturn(saved);

        Pauta result = pautaService.createPauta("Pauta 1", "Descricao", date);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Pauta 1");
        assertThat(result.getDescription()).isEqualTo("Descricao");
        assertThat(result.getDate()).isEqualTo(date);

        ArgumentCaptor<Pauta> captor = ArgumentCaptor.forClass(Pauta.class);
        verify(pautaRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Pauta 1");
    }

    @Test
    void testFindPautaById_WhenExists() {
        LocalDateTime date = LocalDateTime.of(2026, 2, 15, 10, 0);
        Pauta pauta = Pauta.builder()
                .id(1L)
                .title("Pauta 1")
                .description("Descricao")
                .date(date)
                .build();
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));

        Pauta result = pautaService.findById(1L);

        assertThat(result).isEqualTo(pauta);
        verify(pautaRepository).findById(eq(1L));
    }

    @Test
    void testFindPautaById_WhenNotExists() {
        when(pautaRepository.findById(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pautaService.findById(99999L))
                .isInstanceOf(PautaNotFoundException.class)
                .hasMessageContaining("99999");

        verify(pautaRepository).findById(eq(99999L));
    }
}
