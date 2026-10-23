package br.coop.acme.pauta.controller;

import br.coop.acme.pauta.entity.Pauta;
import br.coop.acme.pauta.service.PautaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<PautaCriadaResponse> cadastrar(@Valid @RequestBody PautaRequest request) {
        Pauta pauta = service.cadastrar(request.titulo(), request.descricao());
        return ResponseEntity.status(HttpStatus.CREATED).body(new PautaCriadaResponse(pauta.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PautaResponse> buscar(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(p -> ResponseEntity.ok(new PautaResponse(p.getId(), p.getTitulo(), p.getDescricao())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record PautaRequest(@NotBlank String titulo, String descricao) {
    }

    public record PautaCriadaResponse(Long id) {
    }

    public record PautaResponse(Long id, String titulo, String descricao) {
    }
}
