package com.cooperativa.votacao.application.voto;

import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import com.cooperativa.votacao.domain.pauta.PautaRepository;
import com.cooperativa.votacao.domain.sessao.Sessao;
import com.cooperativa.votacao.domain.sessao.SessaoRepository;
import com.cooperativa.votacao.domain.voto.ResultadoVotacao;
import com.cooperativa.votacao.domain.voto.SessaoNaoAbertaException;
import com.cooperativa.votacao.domain.voto.TipoVoto;
import com.cooperativa.votacao.domain.voto.Voto;
import com.cooperativa.votacao.domain.voto.VotoDuplicadoException;
import com.cooperativa.votacao.domain.voto.VotoRepository;
import java.time.Clock;
import java.util.Comparator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoService {

    private final PautaRepository pautaRepository;
    private final SessaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final Clock clock;

    public VotoService(
            PautaRepository pautaRepository,
            SessaoRepository sessaoRepository,
            VotoRepository votoRepository,
            Clock clock) {
        this.pautaRepository = pautaRepository;
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.clock = clock;
    }

    /**
     * Ordem 404 (pauta) -> 400 (payload) -> 409 (sessao nao aberta) -> 409 (duplicado), por
     * task.md desta fatia (ordem invertida em relacao a SessaoService: ali duracao/400 vem
     * antes da pauta/404). Constraint unica (associado, pauta_id) no banco e a defesa real
     * contra duplicata concorrente — o check em memoria aqui so evita a volta ao banco no
     * caso comum sem concorrencia; a saveAndFlush implicita do IDENTITY ainda pode lancar
     * DataIntegrityViolationException sob corrida, mapeada para o mesmo 409.
     */
    @Transactional
    public Voto registrar(Long pautaId, String associado, TipoVoto tipoVoto) {
        var pauta = pautaRepository.buscarPorId(pautaId)
                .orElseThrow(() -> new PautaNaoEncontradaException(pautaId));

        var voto = new Voto(pauta, associado, tipoVoto);

        if (!existeSessaoAberta(pautaId)) {
            throw new SessaoNaoAbertaException(pautaId);
        }

        var associadoStripado = associado.strip();
        var jaVotou = votoRepository.buscarPorPautaId(pautaId).stream()
                .anyMatch(v -> v.getAssociado().equals(associadoStripado));
        if (jaVotou) {
            throw new VotoDuplicadoException(pautaId, associado);
        }

        try {
            return votoRepository.salvar(voto);
        } catch (DataIntegrityViolationException ex) {
            throw new VotoDuplicadoException(pautaId, associado);
        }
    }

    public ResultadoVotacao resultado(Long pautaId) {
        pautaRepository.buscarPorId(pautaId)
                .orElseThrow(() -> new PautaNaoEncontradaException(pautaId));

        var votos = votoRepository.buscarPorPautaId(pautaId);
        var sim = votos.stream().filter(v -> v.getTipoVoto() == TipoVoto.SIM).count();
        var nao = votos.stream().filter(v -> v.getTipoVoto() == TipoVoto.NAO).count();

        return new ResultadoVotacao(sim, nao, situacaoDaSessaoMaisRecente(pautaId));
    }

    private boolean existeSessaoAberta(Long pautaId) {
        var agora = clock.instant();
        return sessaoMaisRecente(pautaId)
                .map(sessao -> sessao.estaAberta(agora))
                .orElse(false);
    }

    private String situacaoDaSessaoMaisRecente(Long pautaId) {
        return sessaoMaisRecente(pautaId)
                .map(sessao -> sessao.estaAberta(clock.instant())
                        ? ResultadoVotacao.SITUACAO_ABERTA
                        : ResultadoVotacao.SITUACAO_ENCERRADA)
                .orElse(ResultadoVotacao.SITUACAO_NAO_INICIADA);
    }

    private java.util.Optional<Sessao> sessaoMaisRecente(Long pautaId) {
        return sessaoRepository.buscarPorPautaId(pautaId).stream()
                .max(Comparator.comparing(Sessao::getAbertura));
    }
}
