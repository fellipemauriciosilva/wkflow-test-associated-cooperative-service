package br.coop.assembly.controller;

import br.coop.assembly.dto.CreatePautaRequest;
import br.coop.assembly.dto.PautaResponse;
import br.coop.assembly.entity.Pauta;
import br.coop.assembly.service.PautaService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pautas")
public class PautaController {
    private final PautaService service;

    public PautaController(PautaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PautaResponse> createPauta(@Valid @RequestBody CreatePautaRequest request) {
        log.debug("Creating pauta with title: {}", request.getTitle());
        Pauta pauta = service.createPauta(request.getTitle(), request.getDescription());
        PautaResponse response = PautaResponse.from(pauta);
        return ResponseEntity
                .created(URI.create("/pautas/" + pauta.getId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PautaResponse> getPauta(@PathVariable Long id) {
        log.debug("Retrieving pauta with id: {}", id);
        Pauta pauta = service.getPautaById(id);
        return ResponseEntity.ok(PautaResponse.from(pauta));
    }

    @GetMapping
    public ResponseEntity<List<PautaResponse>> getAllPautas() {
        log.debug("Retrieving all pautas");
        List<PautaResponse> pautas = service.getAllPautas()
                .stream()
                .map(PautaResponse::from)
                .toList();
        return ResponseEntity.ok(pautas);
    }
}
