package com.acme.votacao.pauta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultadoVotacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T10:00:00Z");
    private static final Long PAUTA_ID = 1L;

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    private ResultadoVotacaoService sut;

    @BeforeEach
    void setUp() {
        sut = new ResultadoVotacaoService(pautaRepository, sessaoRepository, votoRepository, clock);
    }

    @Test
    @DisplayName("deve lançar PautaNaoEncontradaException quando pauta não existe")
    void calcular_pautaInexistente_lancaExcecao() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(false);

        assertThatThrownBy(() -> sut.calcular(PAUTA_ID)).isInstanceOf(PautaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("status NAO_INICIADA e totais zero quando pauta não tem sessão")
    void calcular_pautaSemSessao_retornaNaoIniciadaComTotaisZero() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        when(sessaoRepository.findFirstByPautaIdOrderByFimDesc(PAUTA_ID)).thenReturn(Optional.empty());
        when(votoRepository.countByPautaIdAndVoto(PAUTA_ID, VotoValor.SIM)).thenReturn(0L);
        when(votoRepository.countByPautaIdAndVoto(PAUTA_ID, VotoValor.NAO)).thenReturn(0L);

        ResultadoVotacaoResponse resultado = sut.calcular(PAUTA_ID);

        assertThat(resultado.status()).isEqualTo(StatusVotacao.NAO_INICIADA);
        assertThat(resultado.totalSim()).isZero();
        assertThat(resultado.totalNao()).isZero();
        assertThat(resultado.pautaId()).isEqualTo(PAUTA_ID);
    }

    @Test
    @DisplayName("status ABERTA quando agora é antes do fim da sessão")
    void calcular_agoraAntesDoFim_retornaAberta() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        Sessao sessao = new Sessao(PAUTA_ID, AGORA.minusSeconds(30), AGORA.plusSeconds(30), 1);
        when(sessaoRepository.findFirstByPautaIdOrderByFimDesc(PAUTA_ID)).thenReturn(Optional.of(sessao));
        when(votoRepository.countByPautaIdAndVoto(PAUTA_ID, VotoValor.SIM)).thenReturn(2L);
        when(votoRepository.countByPautaIdAndVoto(PAUTA_ID, VotoValor.NAO)).thenReturn(1L);

        ResultadoVotacaoResponse resultado = sut.calcular(PAUTA_ID);

        assertThat(resultado.status()).isEqualTo(StatusVotacao.ABERTA);
        assertThat(resultado.totalSim()).isEqualTo(2);
        assertThat(resultado.totalNao()).isEqualTo(1);
    }

    @Test
    @DisplayName("status ENCERRADA quando agora é igual ao fim da sessão (fronteira)")
    void calcular_agoraIgualAoFim_retornaEncerrada() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        Sessao sessao = new Sessao(PAUTA_ID, AGORA.minusSeconds(60), AGORA, 1);
        when(sessaoRepository.findFirstByPautaIdOrderByFimDesc(PAUTA_ID)).thenReturn(Optional.of(sessao));
        when(votoRepository.countByPautaIdAndVoto(PAUTA_ID, VotoValor.SIM)).thenReturn(0L);
        when(votoRepository.countByPautaIdAndVoto(PAUTA_ID, VotoValor.NAO)).thenReturn(0L);

        ResultadoVotacaoResponse resultado = sut.calcular(PAUTA_ID);

        assertThat(resultado.status()).isEqualTo(StatusVotacao.ENCERRADA);
    }

    @Test
    @DisplayName("status ENCERRADA quando agora é depois do fim da sessão")
    void calcular_agoraDepoisDoFim_retornaEncerrada() {
        when(pautaRepository.existsById(PAUTA_ID)).thenReturn(true);
        Sessao sessao = new Sessao(PAUTA_ID, AGORA.minusSeconds(120), AGORA.minusSeconds(60), 1);
        when(sessaoRepository.findFirstByPautaIdOrderByFimDesc(PAUTA_ID)).thenReturn(Optional.of(sessao));
        when(votoRepository.countByPautaIdAndVoto(PAUTA_ID, VotoValor.SIM)).thenReturn(0L);
        when(votoRepository.countByPautaIdAndVoto(PAUTA_ID, VotoValor.NAO)).thenReturn(0L);

        ResultadoVotacaoResponse resultado = sut.calcular(PAUTA_ID);

        assertThat(resultado.status()).isEqualTo(StatusVotacao.ENCERRADA);
    }
}
