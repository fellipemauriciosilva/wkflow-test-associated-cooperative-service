package com.acme.votacao.pauta;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
     * (voto duplicado). Checagem de sessão, checagem de duplicado e insert ocorrem na
     * mesma transação em {@link VotoTransacional#registrar}, atrás de um lock
     * pessimista na pauta (mesma transação, READ_COMMITTED) — sem o lock, duas chamadas
     * concorrentes poderiam ambas passar pela checagem otimista antes de qualquer
     * insert, e só a constraint UNIQUE do banco (ver catch abaixo) impediria o
     * duplicado de fato; com o lock, a segunda chamada só lê sessão/duplicado depois do
     * commit (ou rollback) da primeira.
     * <p>
     * {@code associadoId} chega já normalizado (trim + lowercase, feito no construtor
     * compacto de {@link VotoRequest}, não aqui): sem isso, "associado-1", "associado-1 "
     * e "Associado-1" seriam três linhas distintas e a regra de "um voto por associado"
     * seria burlável só variando a string. Normalizar no DTO (antes do {@code @Size})
     * evita que um valor bruto fora do limite mas normalizado dentro dele seja rejeitado
     * por engano, e evita que a expansão de um caractere ao virar minúsculo (ex.
     * {@code İ}, U+0130) só seja detectada tarde, pelo banco.
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

        String associadoId = request.associadoId();
        try {
            return votoTransacional.registrar(pautaId, associadoId, request.voto());
        } catch (DataIntegrityViolationException e) {
            if (votoRepository.existsByPautaIdAndAssociadoId(pautaId, associadoId)) {
                throw new VotoDuplicadoException(pautaId, associadoId);
            }
            throw e;
        }
    }
}
