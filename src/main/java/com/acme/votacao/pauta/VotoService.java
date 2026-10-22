package com.acme.votacao.pauta;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class VotoService {

    private final PautaRepository pautaRepository;
    private final SessaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final Clock clock;
    private final GanchoConcorrenciaVoto ganchoConcorrencia;

    public VotoService(PautaRepository pautaRepository, SessaoRepository sessaoRepository,
                        VotoRepository votoRepository, Clock clock, GanchoConcorrenciaVoto ganchoConcorrencia) {
        this.pautaRepository = pautaRepository;
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.clock = clock;
        this.ganchoConcorrencia = ganchoConcorrencia;
    }

    /**
     * Ordem de validação (definida no task.md da Fatia 3): 400 (payload, via @Valid no
     * controller, antes deste método) → 404 (pauta) → 422 (sessão não aberta) → 409
     * (voto duplicado). A checagem de duplicado aqui é otimista (evita round-trip ao
     * banco no caminho feliz); a constraint UNIQUE (pauta_id, associado_id) é quem
     * garante a corretude sob concorrência — ver tratamento de
     * DataIntegrityViolationException no catch.
     */
    public Voto votar(Long pautaId, VotoRequest request) {
        if (!pautaRepository.existsById(pautaId)) {
            throw new PautaNaoEncontradaException(pautaId);
        }

        Instant agora = Instant.now(clock);
        if (sessaoRepository.findFirstByPautaIdAndFimAfter(pautaId, agora).isEmpty()) {
            throw new SessaoNaoAbertaException(pautaId);
        }

        if (votoRepository.existsByPautaIdAndAssociadoId(pautaId, request.associadoId())) {
            throw new VotoDuplicadoException(pautaId, request.associadoId());
        }

        ganchoConcorrencia.antesDeSalvar(pautaId, request.associadoId());

        Voto voto = new Voto(pautaId, request.associadoId(), request.voto(), agora);
        try {
            return votoRepository.save(voto);
        } catch (DataIntegrityViolationException e) {
            throw new VotoDuplicadoException(pautaId, request.associadoId());
        }
    }
}
