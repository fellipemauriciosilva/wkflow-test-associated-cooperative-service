package pautas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessaoService {

    private static final int DURACAO_PADRAO_MINUTOS = 1;

    private final SessaoRepository sessaoRepository;
    private final PautaRepository pautaRepository;
    private final Clock clock;

    public SessaoService(SessaoRepository sessaoRepository, PautaRepository pautaRepository, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.pautaRepository = pautaRepository;
        this.clock = clock;
    }

    @Transactional
    public SessaoResponse abrir(Long pautaId, Integer duracaoMinutos) {
        Pauta pauta = pautaRepository.findById(pautaId)
                .orElseThrow(() -> new PautaNaoEncontradaException(pautaId));
        if (sessaoRepository.existsByPautaId(pautaId)) {
            throw new SessaoJaExisteException(pautaId);
        }
        int minutos = duracaoMinutos == null ? DURACAO_PADRAO_MINUTOS : duracaoMinutos;
        Instant abertura = clock.instant();
        Sessao sessao = new Sessao(pauta, abertura, abertura.plus(Duration.ofMinutes(minutos)));
        try {
            return SessaoResponse.de(sessaoRepository.saveAndFlush(sessao));
        } catch (DataIntegrityViolationException e) {
            throw new SessaoJaExisteException(pautaId);
        }
    }
}
