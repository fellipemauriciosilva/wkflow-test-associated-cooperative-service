package com.acme.votacao.pauta;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Checagem de sessão aberta, checagem de duplicado e insert do voto, atomicamente na
 * mesma transação (isolamento READ_COMMITTED, mesmo padrão de {@link SessaoService#abrir}).
 * <p>
 * O lock {@code PESSIMISTIC_WRITE} na linha de {@link Pauta}, adquirido antes das duas
 * checagens, serializa chamadas concorrentes para a mesma pauta: a segunda só adquire o
 * lock depois do commit (ou rollback) da primeira, e nesse ponto sua leitura de sessão e
 * de duplicado já reflete o resultado da primeira (READ_COMMITTED garante que a leitura,
 * feita depois do lock, não usa um snapshot anterior a ele). Isso fecha a janela em que
 * duas chamadas concorrentes veriam ambas "sessão aberta, sem voto prévio" antes de
 * qualquer insert — sem o lock, só a constraint UNIQUE do banco bloquearia o duplicado
 * (ainda corretamente, mas no insert, não na checagem), e a janela de sessão fechando
 * entre a leitura do relógio e o commit continuaria aberta.
 * <p>
 * Separado de {@link VotoService} (que não é transacional) de propósito: se o insert
 * falhar por {@code DataIntegrityViolationException}, {@link VotoService} precisa
 * confirmar o duplicado com uma nova consulta — e isso só é seguro numa transação nova,
 * depois do rollback desta. Reconsultar dentro da mesma transação que acabou de falhar
 * no flush deixa a sessão do Hibernate em estado inválido
 * ({@code HHH000099 AssertionFailure: null id ... don't flush the Session after an
 * exception occurs}). Por isso a fronteira transacional tem que terminar aqui, não em
 * {@code VotoService.votar}.
 */
@Component
public class VotoTransacional {

    private final SessaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final EntityManager entityManager;
    private final Clock clock;
    private final GanchoConcorrenciaVoto ganchoConcorrencia;

    VotoTransacional(SessaoRepository sessaoRepository, VotoRepository votoRepository, EntityManager entityManager,
                      Clock clock, GanchoConcorrenciaVoto ganchoConcorrencia) {
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.entityManager = entityManager;
        this.clock = clock;
        this.ganchoConcorrencia = ganchoConcorrencia;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Voto registrar(Long pautaId, String associadoId, VotoValor votoValor) {
        ganchoConcorrencia.antesDeSalvar(pautaId, associadoId);

        entityManager.find(Pauta.class, pautaId, LockModeType.PESSIMISTIC_WRITE);

        Instant agora = Instant.now(clock);
        if (sessaoRepository.findFirstByPautaIdAndFimAfter(pautaId, agora).isEmpty()) {
            throw new SessaoNaoAbertaException(pautaId);
        }

        if (votoRepository.existsByPautaIdAndAssociadoId(pautaId, associadoId)) {
            throw new VotoDuplicadoException(pautaId, associadoId);
        }

        return votoRepository.save(new Voto(pautaId, associadoId, votoValor, agora));
    }
}
