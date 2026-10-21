package com.votacao.application.voto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.votacao.application.pauta.PautaService;
import com.votacao.domain.pauta.Pauta;
import com.votacao.domain.pauta.PautaNotFoundException;
import com.votacao.domain.sessao.Sessao;
import com.votacao.domain.sessao.SessaoRepository;
import com.votacao.domain.voto.SessaoIndisponivelParaVotoException;
import com.votacao.domain.voto.TipoVoto;
import com.votacao.domain.voto.Voto;
import com.votacao.domain.voto.VotoDuplicadoException;
import com.votacao.domain.voto.VotoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-10-21T10:00:00Z");

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private PautaService pautaService;

    private Clock clock;
    private VotoService sut;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, ZONE);
        sut = new VotoService(votoRepository, sessaoRepository, pautaService, clock);
    }

    private Sessao sessaoAberta(Pauta pauta) {
        LocalDateTime abertura = LocalDateTime.now(clock).minusMinutes(1);
        return new Sessao(pauta, abertura, 5);
    }

    @Test
    @DisplayName("registrar com sessão aberta e payload válido deve persistir o voto")
    void registrar_sessaoAbertaEPayloadValido_persisteVoto() {
        Pauta pauta = new Pauta("Título", "Descrição");
        Sessao sessao = sessaoAberta(pauta);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.existsByPautaIdAndIdAssociado(1L, 10L)).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Voto resultado = sut.registrar(1L, 10L, TipoVoto.SIM);

        assertThat(resultado.getIdAssociado()).isEqualTo(10L);
        assertThat(resultado.getTipo()).isEqualTo(TipoVoto.SIM);
    }

    @Test
    @DisplayName("registrar com voto duplicado do mesmo associado na mesma pauta deve lançar VotoDuplicadoException")
    void registrar_votoDuplicado_lancaVotoDuplicadoException() {
        Pauta pauta = new Pauta("Título", "Descrição");
        Sessao sessao = sessaoAberta(pauta);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.existsByPautaIdAndIdAssociado(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> sut.registrar(1L, 10L, TipoVoto.SIM))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("registrar sem sessão aberta para a pauta deve lançar SessaoIndisponivelParaVotoException")
    void registrar_semSessaoAberta_lancaSessaoIndisponivelParaVotoException() {
        Pauta pauta = new Pauta("Título", "Descrição");
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.registrar(1L, 10L, TipoVoto.SIM))
                .isInstanceOf(SessaoIndisponivelParaVotoException.class);
    }

    @Test
    @DisplayName("registrar com sessão já expirada deve lançar SessaoIndisponivelParaVotoException")
    void registrar_sessaoExpirada_lancaSessaoIndisponivelParaVotoException() {
        Pauta pauta = new Pauta("Título", "Descrição");
        LocalDateTime abertura = LocalDateTime.now(clock).minusMinutes(10);
        Sessao sessao = new Sessao(pauta, abertura, 5);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));

        assertThatThrownBy(() -> sut.registrar(1L, 10L, TipoVoto.SIM))
                .isInstanceOf(SessaoIndisponivelParaVotoException.class);
    }

    @Test
    @DisplayName("registrar com sessão ainda não iniciada deve lançar SessaoIndisponivelParaVotoException")
    void registrar_sessaoNaoIniciada_lancaSessaoIndisponivelParaVotoException() {
        Pauta pauta = new Pauta("Título", "Descrição");
        LocalDateTime abertura = LocalDateTime.now(clock).plusMinutes(5);
        Sessao sessao = new Sessao(pauta, abertura, 5);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));

        assertThatThrownBy(() -> sut.registrar(1L, 10L, TipoVoto.SIM))
                .isInstanceOf(SessaoIndisponivelParaVotoException.class);
    }

    @Test
    @DisplayName("registrar em pauta inexistente deve lançar PautaNotFoundException")
    void registrar_pautaInexistente_lancaPautaNotFoundException() {
        when(pautaService.buscarPorId(999L)).thenThrow(new PautaNotFoundException(999L));

        assertThatThrownBy(() -> sut.registrar(999L, 10L, TipoVoto.SIM))
                .isInstanceOf(PautaNotFoundException.class);
    }

    @Test
    @DisplayName("registrar com violação de integridade da constraint de voto único deve lançar VotoDuplicadoException")
    void registrar_violacaoConstraintVotoUnico_lancaVotoDuplicadoException() {
        Pauta pauta = new Pauta("Título", "Descrição");
        Sessao sessao = sessaoAberta(pauta);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.existsByPautaIdAndIdAssociado(1L, 10L)).thenReturn(false);
        DataIntegrityViolationException excecaoConstraint = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("Unique index or primary key violation: \"" + Voto.UK_PAUTA_ASSOCIADO + "\""));
        when(votoRepository.save(any(Voto.class))).thenThrow(excecaoConstraint);

        assertThatThrownBy(() -> sut.registrar(1L, 10L, TipoVoto.SIM))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("registrar com violação de integridade não relacionada à constraint de voto deve relançar a exceção original")
    void registrar_violacaoDeIntegridadeNaoRelacionada_relancaExcecaoOriginal() {
        Pauta pauta = new Pauta("Título", "Descrição");
        Sessao sessao = sessaoAberta(pauta);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.existsByPautaIdAndIdAssociado(1L, 10L)).thenReturn(false);
        DataIntegrityViolationException excecaoOutraConstraint = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("NULL not allowed column \"TIPO\""));
        when(votoRepository.save(any(Voto.class))).thenThrow(excecaoOutraConstraint);

        assertThatThrownBy(() -> sut.registrar(1L, 10L, TipoVoto.SIM))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("registrar violação com índice autogerado do H2 (CONSTRAINT_INDEX_N) sobre pauta_id/id_associado deve "
            + "lançar VotoDuplicadoException")
    void registrar_violacaoConstraintIndiceAutogerado_lancaVotoDuplicadoException() {
        Pauta pauta = new Pauta("Título", "Descrição");
        Sessao sessao = sessaoAberta(pauta);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.existsByPautaIdAndIdAssociado(1L, 10L)).thenReturn(false);
        DataIntegrityViolationException excecaoIndiceAutogerado = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("Unique index or primary key violation: "
                        + "\"CONSTRAINT_INDEX_9 ON PUBLIC.VOTO(PAUTA_ID, ID_ASSOCIADO) VALUES ( /* 1 */ 1, 10 )\""));
        when(votoRepository.save(any(Voto.class))).thenThrow(excecaoIndiceAutogerado);

        assertThatThrownBy(() -> sut.registrar(1L, 10L, TipoVoto.SIM))
                .isInstanceOf(VotoDuplicadoException.class);
    }

    @Test
    @DisplayName("registrar violação na tabela voto com colunas diferentes do índice autogerado deve relançar "
            + "exceção original")
    void registrar_violacaoConstraintIndiceAutogeradoColunasDiferentes_relancaExcecaoOriginal() {
        Pauta pauta = new Pauta("Título", "Descrição");
        Sessao sessao = sessaoAberta(pauta);
        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.existsByPautaIdAndIdAssociado(1L, 10L)).thenReturn(false);
        DataIntegrityViolationException excecaoColunasDiferentes = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("Unique index or primary key violation: "
                        + "\"CONSTRAINT_INDEX_3 ON PUBLIC.VOTO(OUTRA_COLUNA) VALUES ( /* 1 */ 1 )\""));
        when(votoRepository.save(any(Voto.class))).thenThrow(excecaoColunasDiferentes);

        assertThatThrownBy(() -> sut.registrar(1L, 10L, TipoVoto.SIM))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(VotoDuplicadoException.class);
    }
}
