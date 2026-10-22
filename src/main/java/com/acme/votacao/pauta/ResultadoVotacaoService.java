package com.acme.votacao.pauta;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class ResultadoVotacaoService {

    private final PautaRepository pautaRepository;
    private final SessaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final Clock clock;

    public ResultadoVotacaoService(PautaRepository pautaRepository, SessaoRepository sessaoRepository,
                                    VotoRepository votoRepository, Clock clock) {
        this.pautaRepository = pautaRepository;
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.clock = clock;
    }

    public ResultadoVotacaoResponse calcular(Long pautaId) {
        if (!pautaRepository.existsById(pautaId)) {
            throw new PautaNaoEncontradaException(pautaId);
        }

        long totalSim = votoRepository.countByPautaIdAndVoto(pautaId, VotoValor.SIM);
        long totalNao = votoRepository.countByPautaIdAndVoto(pautaId, VotoValor.NAO);
        StatusVotacao status = calcularStatus(pautaId);
        return new ResultadoVotacaoResponse(pautaId, totalSim, totalNao, status);
    }

    private StatusVotacao calcularStatus(Long pautaId) {
        Optional<Sessao> ultimaSessao = sessaoRepository.findFirstByPautaIdOrderByFimDesc(pautaId);
        if (ultimaSessao.isEmpty()) {
            return StatusVotacao.NAO_INICIADA;
        }
        Instant agora = Instant.now(clock);
        return agora.isBefore(ultimaSessao.get().getFim()) ? StatusVotacao.ABERTA : StatusVotacao.ENCERRADA;
    }
}
