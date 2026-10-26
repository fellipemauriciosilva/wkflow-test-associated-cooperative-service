package br.acme.wkflow.voto;

import br.acme.wkflow.pauta.PautaNaoEncontradaException;
import br.acme.wkflow.pauta.PautaRepository;
import br.acme.wkflow.sessao.SessaoRepository;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas/{id}")
public class VotoController {

    private final PautaRepository pautas;
    private final SessaoRepository sessoes;
    private final VotoRepository votos;
    private final Clock clock;

    public VotoController(PautaRepository pautas, SessaoRepository sessoes, VotoRepository votos, Clock clock) {
        this.pautas = pautas;
        this.sessoes = sessoes;
        this.votos = votos;
        this.clock = clock;
    }

    @PostMapping("/votos")
    @Transactional
    public ResponseEntity<VotoResponse> votar(@PathVariable Long id, @Valid @RequestBody VotoRequest request) {
        if (!pautas.existsById(id)) {
            throw new PautaNaoEncontradaException(id);
        }
        Instant agora = Instant.now(clock);
        if (!sessoes.existsByPautaIdAndFimGreaterThanEqual(id, agora)) {
            throw new SessaoNaoAbertaException(id);
        }
        ValorVoto valor = ValorVoto.de(request.valor());
        try {
            votos.saveAndFlush(new Voto(id, request.associadoId(), valor));
        } catch (DataIntegrityViolationException ex) {
            throw new VotoDuplicadoException(id, request.associadoId());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new VotoResponse(id, request.associadoId(), valor));
    }

    @GetMapping("/resultado")
    @Transactional(readOnly = true)
    public ResultadoResponse resultado(@PathVariable Long id) {
        if (!pautas.existsById(id)) {
            throw new PautaNaoEncontradaException(id);
        }
        long sim = votos.countByPautaIdAndValor(id, ValorVoto.SIM);
        long nao = votos.countByPautaIdAndValor(id, ValorVoto.NAO);
        return new ResultadoResponse(sim, nao, sim + nao);
    }
}
