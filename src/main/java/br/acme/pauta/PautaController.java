package br.acme.pauta;

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

    private final PautaService service;

    public PautaController(PautaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PautaResponse> criar(@Valid @RequestBody PautaRequest request) {
        PautaResponse criada = service.criar(request);
        return ResponseEntity.created(URI.create("/pautas/" + criada.id())).body(criada);
    }

    @GetMapping("/{id}")
    public PautaResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }
}
