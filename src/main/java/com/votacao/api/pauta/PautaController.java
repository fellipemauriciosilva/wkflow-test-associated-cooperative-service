package com.votacao.api.pauta;

import com.votacao.api.pauta.dto.PautaRequest;
import com.votacao.api.pauta.dto.PautaResponse;
import com.votacao.application.pauta.PautaService;
import com.votacao.domain.pauta.Pauta;
import jakarta.validation.Valid;
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

    private final PautaService pautaService;

    public PautaController(PautaService pautaService) {
        this.pautaService = pautaService;
    }

    @PostMapping
    public ResponseEntity<PautaResponse> criar(@Valid @RequestBody PautaRequest request) {
        Pauta pauta = pautaService.criar(request.titulo(), request.descricao());
        return ResponseEntity.status(HttpStatus.CREATED).body(PautaResponse.from(pauta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PautaResponse> buscarPorId(@PathVariable Long id) {
        Pauta pauta = pautaService.buscarPorId(id);
        return ResponseEntity.ok(PautaResponse.from(pauta));
    }
}
