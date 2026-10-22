package com.acme.votacao.pauta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T10:00:00Z");
    private static final Long PAUTA_ID = 1L;
    private static final String ASSOCIADO_ID = "associado-1";

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private VotoTransacional votoTransacional;

    private VotoService sut;

    @BeforeEach
    void setUp() {
        sut = new VotoService(pautaRepository, votoRepository, votoTransacional);
    }

    @Test
    @DisplayName("deve registrar voto quando pauta existe, delegando a checagem de sessão/duplicado/insert ao componente transacional")
    void votar_pautaExistente_delegaParaVotoTransacional() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        Voto votoEsperado = new Voto(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM, AGORA);
        when(votoTransacional.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM)).thenReturn(votoEsperado);

        Voto resultado = sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM));

        assertThat(resultado).isSameAs(votoEsperado);
    }

    @Test
    @DisplayName("deve lançar PautaNaoEncontradaException quando pauta não existe, sem chamar o componente transacional")
    void votar_pautaInexistente_lancaExcecaoSemChamarVotoTransacional() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(false);

        assertThatThrownBy(() -> sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM)))
                .isInstanceOf(PautaNaoEncontradaException.class);

        verify(votoTransacional, org.mockito.Mockito.never()).registrar(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("deve propagar SessaoNaoAbertaException lançada pelo componente transacional")
    void votar_votoTransacionalLancaSessaoNaoAberta_propagaExcecao() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        when(votoTransacional.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM))
                .thenThrow(new SessaoNaoAbertaException(PAUTA_ID));

        assertThatThrownBy(() -> sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM)))
                .isInstanceOf(SessaoNaoAbertaException.class);
    }

    @Test
    @DisplayName("deve propagar VotoDuplicadoException lançada pelo componente transacional (checagem otimista)")
    void votar_votoTransacionalLancaVotoDuplicado_propagaExcecao() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        when(votoTransacional.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM))
                .thenThrow(new VotoDuplicadoException(PAUTA_ID, ASSOCIADO_ID));

        assertThatThrownBy(() -> sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM)))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("deve lançar VotoDuplicadoException quando o insert falha por integridade e a releitura (fora da transação que falhou) confirma duplicado")
    void votar_votoTransacionalFalhaPorIntegridadeEReleituraConfirmaDuplicado_lancaVotoDuplicadoException() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        when(votoTransacional.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));
        when(votoRepository.existsByPautaIdAndAssociadoId(PAUTA_ID, ASSOCIADO_ID)).thenReturn(true);

        assertThatThrownBy(() -> sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM)))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("deve relançar a exceção original quando o insert falha por integridade mas a releitura não confirma duplicado (outra violação)")
    void votar_votoTransacionalFalhaPorIntegridadeMasReleituraNaoConfirmaDuplicado_relancaExcecaoOriginal() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        DataIntegrityViolationException excecaoOriginal = new DataIntegrityViolationException("outra violação de integridade");
        when(votoTransacional.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM)).thenThrow(excecaoOriginal);
        when(votoRepository.existsByPautaIdAndAssociadoId(PAUTA_ID, ASSOCIADO_ID)).thenReturn(false);

        assertThatThrownBy(() -> sut.votar(PAUTA_ID, new VotoRequest(ASSOCIADO_ID, VotoValor.SIM)))
                .isSameAs(excecaoOriginal);
    }

    @Test
    @DisplayName("deve normalizar associadoId (trim e lowercase) antes de delegar ao componente transacional")
    void votar_associadoIdComEspacosEMaiusculas_normalizaAntesDeDelegar() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        Voto votoEsperado = new Voto(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM, AGORA);
        when(votoTransacional.registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM)).thenReturn(votoEsperado);

        sut.votar(PAUTA_ID, new VotoRequest(" Associado-1 ", VotoValor.SIM));

        verify(votoTransacional).registrar(PAUTA_ID, ASSOCIADO_ID, VotoValor.SIM);
    }
}
