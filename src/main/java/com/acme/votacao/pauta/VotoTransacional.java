package com.acme.votacao.pauta;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Checagem de sessão aberta, checagem de duplicado e insert do voto, atomicamente na
 * mesma transação (isolamento READ_COMMITTED, mesmo padrão de
 * {@link SessaoService#abrir}) — elimina a janela em que duas chamadas concorrentes
 * veriam ambas "sem voto" antes de qualquer insert.
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
class VotoTransacional {

    private final SessaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final Clock clock;
    private final GanchoConcorrenciaVoto ganchoConcorrencia;

    VotoTransacional(SessaoRepository sessaoRepository, VotoRepository votoRepository, Clock clock,
                      GanchoConcorrenciaVoto ganchoConcorrencia) {
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.clock = clock;
        this.ganchoConcorrencia = ganchoConcorrencia;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    Voto registrar(Long pautaId, String associadoId, VotoValor votoValor) {
        Instant agora = Instant.now(clock);
        if (sessaoRepository.findFirstByPautaIdAndFimAfter(pautaId, agora).isEmpty()) {
            throw new SessaoNaoAbertaException(pautaId);
        }

        if (votoRepository.existsByPautaIdAndAssociadoId(pautaId, associadoId)) {
            throw new VotoDuplicadoException(pautaId, associadoId);
        }

        ganchoConcorrencia.antesDeSalvar(pautaId, associadoId);

        return votoRepository.save(new Voto(pautaId, associadoId, votoValor, agora));
    }
}
