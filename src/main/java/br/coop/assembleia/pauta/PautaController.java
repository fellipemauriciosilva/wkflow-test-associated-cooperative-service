package br.coop.assembleia.pauta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/pautas")
public class PautaController {

    private final PautaRepository repository;

    public PautaController(PautaRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<PautaIdResponse> criar(@Valid @RequestBody CriarPautaRequest request) {
        Pauta pauta = repository.save(new Pauta(request.titulo(), request.descricao()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new PautaIdResponse(pauta.getId()));
    }

    @GetMapping("/{id}")
    public PautaResponse buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(PautaResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
