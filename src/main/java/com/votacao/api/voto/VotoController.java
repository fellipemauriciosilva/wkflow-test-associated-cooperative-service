package com.votacao.api.voto;

import com.votacao.api.voto.dto.VotoRequest;
import com.votacao.api.voto.dto.VotoResponse;
import com.votacao.application.voto.VotoService;
import com.votacao.domain.voto.Voto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas/{id}/votos")
public class VotoController {

    private final VotoService votoService;

    public VotoController(VotoService votoService) {
        this.votoService = votoService;
    }

    @PostMapping
    public ResponseEntity<VotoResponse> registrar(@PathVariable Long id, @Valid @RequestBody VotoRequest request) {
        Voto voto = votoService.registrar(id, request.idAssociado(), request.voto());
        return ResponseEntity.status(HttpStatus.CREATED).body(VotoResponse.from(voto));
    }
}
