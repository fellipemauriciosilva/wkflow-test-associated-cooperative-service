package com.acme.votacao.pauta;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    public SessaoService(SessaoRepository sessaoRepository, EntityManager entityManager, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /**
     * Lock pessimista na linha de {@link Pauta} serializa aberturas concorrentes para a
     * mesma pauta: a segunda transação só adquire o lock após o commit da primeira, e
     * nesse ponto já enxerga a sessão recém-criada via findFirstByPautaIdAndFimAfter.
     */
    @Transactional
    public Sessao abrir(Long pautaId, SessaoRequest request) {
        Pauta pauta = entityManager.find(Pauta.class, pautaId, LockModeType.PESSIMISTIC_WRITE);
        if (pauta == null) {
            throw new PautaNaoEncontradaException(pautaId);
        }

        Instant agora = Instant.now(clock);
        if (sessaoRepository.findFirstByPautaIdAndFimAfter(pautaId, agora).isPresent()) {
            throw new SessaoJaAbertaException(pautaId);
        }

        int duracaoMinutos = request.duracaoMinutosOuPadrao();
        Instant fim = agora.plus(duracaoMinutos, ChronoUnit.MINUTES);
        Sessao sessao = new Sessao(pautaId, agora, fim, duracaoMinutos);
        return sessaoRepository.save(sessao);
    }
}
