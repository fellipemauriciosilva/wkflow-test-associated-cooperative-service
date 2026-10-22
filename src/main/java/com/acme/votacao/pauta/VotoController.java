package com.acme.votacao.pauta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas")
public class VotoController {

    private final VotoService votoService;

    public VotoController(VotoService votoService) {
        this.votoService = votoService;
    }

    @PostMapping("/{id}/votos")
    public ResponseEntity<VotoResponse> votar(@PathVariable Long id, @Valid @RequestBody VotoRequest request) {
        Voto voto = votoService.votar(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(VotoResponse.from(voto));
    }
}
