package com.cooperative.voting.controller;

import com.cooperative.voting.dto.PautaRequest;
import com.cooperative.voting.entity.Pauta;
import com.cooperative.voting.service.PautaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST endpoints for managing assembly agendas (pautas).
 */
@Slf4j
@RestController
@RequestMapping("/pautas")
@RequiredArgsConstructor
public class PautaController {

    private final PautaService pautaService;

    /**
     * Creates a new pauta.
     *
     * @param request validated request payload
     * @return 201 Created with Location header and the persisted entity
     */
    @PostMapping
    public ResponseEntity<Pauta> createPauta(@Valid @RequestBody PautaRequest request) {
        log.info("Received request to create pauta: {}", request.getTitle());
        Pauta created = pautaService.createPauta(request.getTitle(), request.getDescription(), request.getDate());
        return ResponseEntity.created(URI.create("/pautas/" + created.getId())).body(created);
    }

    /**
     * Retrieves a pauta by id.
     *
     * @param id pauta identifier
     * @return 200 OK with the entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<Pauta> getPautaById(@PathVariable Long id) {
        log.info("Received request to get pauta with id: {}", id);
        Pauta pauta = pautaService.findById(id);
        return ResponseEntity.ok(pauta);
    }
}
