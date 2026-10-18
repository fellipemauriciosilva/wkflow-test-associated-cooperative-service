package com.example.pauta.controller;

import com.example.pauta.dto.PautaRequest;
import com.example.pauta.dto.PautaResponse;
import com.example.pauta.entity.Pauta;
import com.example.pauta.service.PautaService;
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

@Slf4j
@RestController
@RequestMapping("/pautas")
@RequiredArgsConstructor
public class PautaController {

    private final PautaService pautaService;

    @PostMapping
    public ResponseEntity<PautaResponse> create(@Valid @RequestBody PautaRequest request) {
        log.info("Creating pauta with title '{}'", request.title());
        Pauta pauta = pautaService.create(request.title());
        URI location = URI.create("/pautas/" + pauta.getId());
        return ResponseEntity.created(location).body(PautaResponse.fromEntity(pauta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PautaResponse> findById(@PathVariable Long id) {
        log.info("Retrieving pauta with id {}", id);
        Pauta pauta = pautaService.findById(id);
        return ResponseEntity.ok(PautaResponse.fromEntity(pauta));
    }
}
