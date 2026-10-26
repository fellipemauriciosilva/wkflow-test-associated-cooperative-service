package br.acme.wkflow.sessao;

import br.acme.wkflow.pauta.PautaNaoEncontradaException;
import br.acme.wkflow.pauta.PautaRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas/{id}/sessao")
public class SessaoController {

    private static final long DURACAO_PADRAO_MINUTOS = 1;

    private final PautaRepository pautas;
    private final SessaoRepository sessoes;
    private final Clock clock;

    public SessaoController(PautaRepository pautas, SessaoRepository sessoes, Clock clock) {
        this.pautas = pautas;
        this.sessoes = sessoes;
        this.clock = clock;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<SessaoResponse> abrir(@PathVariable Long id,
            @Valid @RequestBody(required = false) SessaoRequest request) {
        pautas.buscarComLock(id).orElseThrow(() -> new PautaNaoEncontradaException(id));
        Instant inicio = Instant.now(clock);
        if (sessoes.existsByPautaIdAndFimAfter(id, inicio)) {
            throw new SessaoJaAbertaException(id);
        }
        long duracao = request == null || request.duracaoMinutos() == null
                ? DURACAO_PADRAO_MINUTOS
                : request.duracaoMinutos();
        Sessao salva = sessoes.save(new Sessao(id, inicio, inicio.plus(duracao, ChronoUnit.MINUTES)));
        return ResponseEntity.status(HttpStatus.CREATED).body(SessaoResponse.de(salva));
    }
}
