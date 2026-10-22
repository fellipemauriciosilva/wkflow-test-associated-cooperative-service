package com.cooperativa.votacao.application.voto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cooperativa.votacao.domain.pauta.Pauta;
import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import com.cooperativa.votacao.domain.pauta.PautaRepository;
import com.cooperativa.votacao.domain.sessao.Sessao;
import com.cooperativa.votacao.domain.sessao.SessaoRepository;
import com.cooperativa.votacao.domain.voto.ResultadoVotacao;
import com.cooperativa.votacao.domain.voto.SessaoNaoAbertaException;
import com.cooperativa.votacao.domain.voto.TipoVoto;
import com.cooperativa.votacao.domain.voto.Voto;
import com.cooperativa.votacao.domain.voto.VotoDuplicadoException;
import com.cooperativa.votacao.domain.voto.VotoInvalidoException;
import com.cooperativa.votacao.domain.voto.VotoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    private static final Instant ABERTURA = Instant.parse("2026-10-22T10:00:00Z");

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    private Clock clock;

    private VotoService sut;

    private Pauta pauta;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(ABERTURA.plusSeconds(30), ZoneOffset.UTC);
        sut = new VotoService(pautaRepository, sessaoRepository, votoRepository, clock);
        pauta = new Pauta("Eleicao de diretoria", null);
    }

    @Test
    @DisplayName("AC-3.1: registra voto SIM com sessao aberta")
    void registrar_comSessaoAbertaEVotoSim_salvaVoto() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Sessao(pauta, 5, ABERTURA)));
        when(votoRepository.buscarPorPautaId(1L)).thenReturn(List.of());
        when(votoRepository.salvar(any(Voto.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var voto = sut.registrar(1L, "associado-A", TipoVoto.SIM);

        assertThat(voto.getTipoVoto()).isEqualTo(TipoVoto.SIM);
        assertThat(voto.getAssociado()).isEqualTo("associado-A");
    }

    @Test
    @DisplayName("AC-3.8: pauta inexistente retorna PautaNaoEncontradaException")
    void registrar_comPautaInexistente_lancaExcecao() {
        when(pautaRepository.buscarPorId(999999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.registrar(999999L, "associado-A", TipoVoto.SIM))
                .isInstanceOf(PautaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("Ordem 404 antes de 400: pauta inexistente com associado invalido ainda lanca PautaNaoEncontradaException")
    void registrar_comPautaInexistenteEAssociadoInvalido_lancaPautaNaoEncontrada() {
        when(pautaRepository.buscarPorId(999999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.registrar(999999L, null, TipoVoto.SIM))
                .isInstanceOf(PautaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("AC-3.9: associado nulo retorna VotoInvalidoException")
    void registrar_comAssociadoNulo_lancaExcecao() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));

        assertThatThrownBy(() -> sut.registrar(1L, null, TipoVoto.SIM))
                .isInstanceOf(VotoInvalidoException.class);
    }

    @Test
    @DisplayName("AC-3.9: associado acima de 50 caracteres retorna VotoInvalidoException")
    void registrar_comAssociadoMuitoLongo_lancaExcecao() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));

        assertThatThrownBy(() -> sut.registrar(1L, "a".repeat(51), TipoVoto.SIM))
                .isInstanceOf(VotoInvalidoException.class);
    }

    @Test
    @DisplayName("AC-3.3: pauta sem nenhuma sessao retorna SessaoNaoAbertaException")
    void registrar_semSessao_lancaExcecao() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> sut.registrar(1L, "associado-A", TipoVoto.SIM))
                .isInstanceOf(SessaoNaoAbertaException.class);
    }

    @Test
    @DisplayName("AC-3.4: voto no instante exato do fim (fim exclusivo) retorna SessaoNaoAbertaException")
    void registrar_comInstanteIgualAoFim_lancaExcecao() {
        var clockNoFim = Clock.fixed(ABERTURA.plusSeconds(60), ZoneOffset.UTC);
        sut = new VotoService(pautaRepository, sessaoRepository, votoRepository, clockNoFim);
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Sessao(pauta, 1, ABERTURA)));

        assertThatThrownBy(() -> sut.registrar(1L, "associado-A", TipoVoto.SIM))
                .isInstanceOf(SessaoNaoAbertaException.class);
    }

    @Test
    @DisplayName("AC-3.5: segundo voto do mesmo associado na mesma pauta retorna VotoDuplicadoException")
    void registrar_comAssociadoQueJaVotou_lancaExcecao() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Sessao(pauta, 5, ABERTURA)));
        when(votoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Voto(pauta, "associado-A", TipoVoto.SIM)));

        assertThatThrownBy(() -> sut.registrar(1L, "associado-A", TipoVoto.NAO))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("Should: duplicata concorrente detectada so pela constraint mapeia para VotoDuplicadoException")
    void registrar_comConstraintViolada_lancaVotoDuplicado() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Sessao(pauta, 5, ABERTURA)));
        when(votoRepository.buscarPorPautaId(1L)).thenReturn(List.of());
        when(votoRepository.salvar(any(Voto.class)))
                .thenThrow(new DataIntegrityViolationException("uk_voto_associado_pauta"));

        assertThatThrownBy(() -> sut.registrar(1L, "associado-A", TipoVoto.SIM))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("Catch da constraint nao mascara outra DataIntegrityViolationException: deve propagar, nao virar 409")
    void registrar_comViolacaoDeOutraColuna_relanca() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Sessao(pauta, 5, ABERTURA)));
        when(votoRepository.buscarPorPautaId(1L)).thenReturn(List.of());
        when(votoRepository.salvar(any(Voto.class)))
                .thenThrow(new DataIntegrityViolationException("NULL not allowed for column \"TIPO_VOTO\""));

        assertThatThrownBy(() -> sut.registrar(1L, "associado-A", TipoVoto.SIM))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("AC-3.5: duplicata detectada apos strip - 'associado-A ' colide com 'associado-A' ja votado")
    void registrar_comAssociadoComEspacoQueJaVotouStripado_lancaExcecao() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Sessao(pauta, 5, ABERTURA)));
        when(votoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Voto(pauta, "associado-A", TipoVoto.SIM)));

        assertThatThrownBy(() -> sut.registrar(1L, "associado-A ", TipoVoto.NAO))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("AC-3.6: resultado conta SIM e NAO e situacao aberta")
    void resultado_comSessaoAberta_contaVotosESituacaoAberta() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Sessao(pauta, 5, ABERTURA)));
        when(votoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(
                        new Voto(pauta, "associado-A", TipoVoto.SIM),
                        new Voto(pauta, "associado-B", TipoVoto.SIM),
                        new Voto(pauta, "associado-C", TipoVoto.NAO)));

        var resultado = sut.resultado(1L);

        assertThat(resultado.sim()).isEqualTo(2);
        assertThat(resultado.nao()).isEqualTo(1);
        assertThat(resultado.situacao()).isEqualTo(ResultadoVotacao.SITUACAO_ABERTA);
    }

    @Test
    @DisplayName("AC-3.6: resultado com sessao encerrada reporta situacao encerrada")
    void resultado_comSessaoEncerrada_situacaoEncerrada() {
        var clockDepoisDoFim = Clock.fixed(ABERTURA.plusSeconds(120), ZoneOffset.UTC);
        sut = new VotoService(pautaRepository, sessaoRepository, votoRepository, clockDepoisDoFim);
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(new Sessao(pauta, 1, ABERTURA)));
        when(votoRepository.buscarPorPautaId(1L)).thenReturn(List.of());

        var resultado = sut.resultado(1L);

        assertThat(resultado.situacao()).isEqualTo(ResultadoVotacao.SITUACAO_ENCERRADA);
    }

    @Test
    @DisplayName("AC-3.6: resultado usa a sessao mais recente da pauta")
    void resultado_comVariasSessoes_usaMaisRecente() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L))
                .thenReturn(List.of(
                        new Sessao(pauta, 1, ABERTURA.minusSeconds(3600)),
                        new Sessao(pauta, 5, ABERTURA)));
        when(votoRepository.buscarPorPautaId(1L)).thenReturn(List.of());

        var resultado = sut.resultado(1L);

        assertThat(resultado.situacao()).isEqualTo(ResultadoVotacao.SITUACAO_ABERTA);
    }

    @Test
    @DisplayName("Dúvida 2: resultado sem nenhuma sessao retorna zeros e situacao nao_iniciada")
    void resultado_semSessao_retornaZerosESituacaoNaoIniciada() {
        when(pautaRepository.buscarPorId(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.buscarPorPautaId(1L)).thenReturn(List.of());
        when(votoRepository.buscarPorPautaId(1L)).thenReturn(List.of());

        var resultado = sut.resultado(1L);

        assertThat(resultado.sim()).isZero();
        assertThat(resultado.nao()).isZero();
        assertThat(resultado.situacao()).isEqualTo(ResultadoVotacao.SITUACAO_NAO_INICIADA);
    }

    @Test
    @DisplayName("AC-3.7: resultado de pauta inexistente lanca PautaNaoEncontradaException")
    void resultado_comPautaInexistente_lancaExcecao() {
        when(pautaRepository.buscarPorId(999999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.resultado(999999L))
                .isInstanceOf(PautaNaoEncontradaException.class);
    }
}
