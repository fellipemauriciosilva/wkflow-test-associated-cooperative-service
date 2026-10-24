package com.acme.pautas.service;

import com.acme.pautas.domain.Sessao;
import com.acme.pautas.repository.PautaRepository;
import com.acme.pautas.repository.SessaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class SessaoService {

    private final PautaRepository pautas;
    private final SessaoRepository sessoes;
    private final Clock clock;

    public SessaoService(PautaRepository pautas, SessaoRepository sessoes, Clock clock) {
        this.pautas = pautas;
        this.sessoes = sessoes;
        this.clock = clock;
    }

    @Transactional
    public Sessao abrir(Long pautaId, int duracaoMinutos) {
        pautas.findWithLockById(pautaId).orElseThrow(() -> new PautaNaoEncontradaException(pautaId));
        Instant agora = Instant.now(clock);
        if (sessoes.existsByPautaIdAndFimAfter(pautaId, agora)) {
            throw new SessaoJaAbertaException(pautaId);
        }
        return sessoes.save(new Sessao(pautaId, agora, agora.plus(Duration.ofMinutes(duracaoMinutos))));
    }
}
