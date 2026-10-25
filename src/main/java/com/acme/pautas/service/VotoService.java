package com.acme.pautas.service;

import com.acme.pautas.domain.OpcaoVoto;
import com.acme.pautas.domain.Voto;
import com.acme.pautas.repository.PautaRepository;
import com.acme.pautas.repository.SessaoRepository;
import com.acme.pautas.repository.VotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class VotoService {

    private final PautaRepository pautas;
    private final SessaoRepository sessoes;
    private final VotoRepository votos;
    private final Clock clock;

    public VotoService(PautaRepository pautas, SessaoRepository sessoes, VotoRepository votos, Clock clock) {
        this.pautas = pautas;
        this.sessoes = sessoes;
        this.votos = votos;
        this.clock = clock;
    }

    @Transactional
    public Voto registrar(Long pautaId, String associadoId, OpcaoVoto opcao) {
        pautas.findById(pautaId).orElseThrow(() -> new PautaNaoEncontradaException(pautaId));
        if (!sessoes.existsByPautaIdAndFimAfter(pautaId, Instant.now(clock))) {
            throw new SemSessaoAbertaException(pautaId);
        }
        if (votos.existsByPautaIdAndAssociadoId(pautaId, associadoId)) {
            throw new VotoDuplicadoException(pautaId, associadoId);
        }
        return votos.save(new Voto(pautaId, associadoId, opcao));
    }

    @Transactional(readOnly = true)
    public ResultadoVotacao resultado(Long pautaId) {
        if (!pautas.existsById(pautaId)) {
            throw new PautaNaoEncontradaException(pautaId);
        }
        long sim = 0;
        long nao = 0;
        for (Object[] linha : votos.contarPorOpcao(pautaId)) {
            OpcaoVoto opcao = (OpcaoVoto) linha[0];
            long total = (Long) linha[1];
            if (opcao == OpcaoVoto.SIM) {
                sim = total;
            } else {
                nao = total;
            }
        }
        return new ResultadoVotacao(sim, nao);
    }
}
