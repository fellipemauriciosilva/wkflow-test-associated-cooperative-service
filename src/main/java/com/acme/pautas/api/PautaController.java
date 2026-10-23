package com.acme.pautas.api;

import com.acme.pautas.domain.Pauta;
import com.acme.pautas.domain.PautaService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/pautas")
public class PautaController {

    private final PautaService service;

    public PautaController(PautaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PautaResponse> criar(@Valid @RequestBody CriarPautaRequest request) {
        Pauta pauta = service.criar(request.titulo(), request.descricao());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(pauta.getId())
                .toUri();
        return ResponseEntity.created(location).body(PautaResponse.de(pauta));
    }

    @GetMapping("/{id}")
    public PautaResponse buscar(@PathVariable Long id) {
        return PautaResponse.de(service.buscarPorId(id));
    }
}
