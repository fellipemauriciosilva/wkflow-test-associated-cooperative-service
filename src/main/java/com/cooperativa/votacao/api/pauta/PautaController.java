package com.cooperativa.votacao.api.pauta;

import com.cooperativa.votacao.api.pauta.dto.PautaRequest;
import com.cooperativa.votacao.api.pauta.dto.PautaResponse;
import com.cooperativa.votacao.application.pauta.PautaService;
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
    public ResponseEntity<PautaResponse> criar(@RequestBody PautaRequest request) {
        var pauta = pautaService.criar(request.titulo(), request.descricao());
        return ResponseEntity.status(HttpStatus.CREATED).body(PautaResponse.de(pauta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PautaResponse> buscarPorId(@PathVariable Long id) {
        var pauta = pautaService.buscarPorId(id);
        return ResponseEntity.ok(PautaResponse.de(pauta));
    }
}
