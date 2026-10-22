package com.acme.votacao.pauta;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class VotoService {

    private final PautaRepository pautaRepository;
    private final VotoRepository votoRepository;
    private final VotoTransacional votoTransacional;

    public VotoService(PautaRepository pautaRepository, VotoRepository votoRepository,
                        VotoTransacional votoTransacional) {
        this.pautaRepository = pautaRepository;
        this.votoRepository = votoRepository;
        this.votoTransacional = votoTransacional;
    }

    /**
     * Ordem de validação (definida no task.md da Fatia 3): 400 (payload, via @Valid no
     * controller, antes deste método) → 404 (pauta) → 422 (sessão não aberta) → 409
     * (voto duplicado). Checagem de sessão, checagem de duplicado e insert ocorrem
     * atomicamente em {@link VotoTransacional#registrar} (mesma transação,
     * READ_COMMITTED) — sem essa atomicidade, a janela entre o check e o save deixaria
     * um voto passar depois de {@code fim} ou dois votos concorrentes passarem ambos
     * pela checagem otimista (violação de AC-3.3 e RF-07, respectivamente).
     * <p>
     * {@code associadoId} é normalizado (trim + lowercase) antes de qualquer checagem e
     * antes do save: sem isso, "associado-1", "associado-1 " e "Associado-1" seriam três
     * linhas distintas e a regra de "um voto por associado" seria burlável só variando a
     * string (decisão de normalização registrada em docs/CONTEXT.md).
     * <p>
     * Se o insert falhar por {@link DataIntegrityViolationException} (constraint UNIQUE
     * sob concorrência real — a checagem otimista dentro da transação não viu o voto da
     * outra transação ainda não commitada), a transação de {@code VotoTransacional} já
     * sofreu rollback nesse ponto. O duplicado é então confirmado aqui, numa transação
     * nova (fora da que falhou — reconsultar na mesma transação que acabou de falhar no
     * flush deixa a sessão do Hibernate em estado inválido): se confirmado, é a
     * constraint UNIQUE (outra requisição venceu a corrida) e vira 409; se não, a
     * violação tem outra causa e é relançada em vez de mascarada como duplicado.
     */
    public Voto votar(Long pautaId, VotoRequest request) {
        if (!pautaRepository.existsById(pautaId)) {
            throw new PautaNaoEncontradaException(pautaId);
        }

        String associadoId = normalizar(request.associadoId());
        try {
            return votoTransacional.registrar(pautaId, associadoId, request.voto());
        } catch (DataIntegrityViolationException e) {
            if (votoRepository.existsByPautaIdAndAssociadoId(pautaId, associadoId)) {
                throw new VotoDuplicadoException(pautaId, associadoId);
            }
            throw e;
        }
    }

    private String normalizar(String associadoId) {
        return associadoId.trim().toLowerCase(Locale.ROOT);
    }
}
