package br.acme.pauta;

import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoService {

    private final PautaRepository pautas;
    private final SessaoRepository sessoes;
    private final VotoRepository votos;
    private final Clock relogio;

    public VotoService(PautaRepository pautas, SessaoRepository sessoes, VotoRepository votos, Clock relogio) {
        this.pautas = pautas;
        this.sessoes = sessoes;
        this.votos = votos;
        this.relogio = relogio;
    }

    @Transactional
    public VotoResponse registrar(Long pautaId, VotoRequest request) {
        Pauta pauta = pautas.findComLockPorId(pautaId)
                .orElseThrow(() -> new PautaNaoEncontradaException(pautaId));
        Instant agora = relogio.instant();
        Sessao sessao = sessoes.findAbertaEm(pautaId, agora)
                .orElseThrow(() -> new SemSessaoAbertaException(pautaId));
        if (votos.existsByPautaIdAndAssociadoId(pautaId, request.associadoId())) {
            throw new VotoDuplicadoException(pautaId);
        }
        try {
            Voto voto = votos.saveAndFlush(new Voto(pauta, sessao, request.associadoId(),
                    OpcaoVoto.valueOf(request.opcao()), agora));
            return VotoResponse.de(voto);
        } catch (DataIntegrityViolationException e) {
            throw new VotoDuplicadoException(pautaId);
        }
    }

    @Transactional(readOnly = true)
    public ResultadoResponse resultado(Long pautaId) {
        if (!pautas.existsById(pautaId)) {
            throw new PautaNaoEncontradaException(pautaId);
        }
        if (sessoes.countAbertasEm(pautaId, relogio.instant()) > 0) {
            throw new ResultadoIndisponivelException(pautaId);
        }
        long sim = votos.countByPautaIdAndOpcao(pautaId, OpcaoVoto.SIM);
        long nao = votos.countByPautaIdAndOpcao(pautaId, OpcaoVoto.NAO);
        return new ResultadoResponse(sim, nao, sim + nao);
    }
}
