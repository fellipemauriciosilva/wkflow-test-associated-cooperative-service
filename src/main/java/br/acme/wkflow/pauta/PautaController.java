package br.acme.wkflow.pauta;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas")
public class PautaController {

    private final PautaRepository pautas;

    public PautaController(PautaRepository pautas) {
        this.pautas = pautas;
    }

    @PostMapping
    public ResponseEntity<PautaCriadaResponse> criar(@Valid @RequestBody PautaRequest request) {
        Pauta salva = pautas.save(new Pauta(request.descricao()));
        return ResponseEntity.created(URI.create("/pautas/" + salva.getId()))
                .body(new PautaCriadaResponse(salva.getId()));
    }

    @GetMapping("/{id}")
    public PautaResponse buscar(@PathVariable Long id) {
        return pautas.findById(id)
                .map(PautaResponse::de)
                .orElseThrow(() -> new PautaNaoEncontradaException(id));
    }
}
