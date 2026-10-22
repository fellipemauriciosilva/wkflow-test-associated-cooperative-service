package com.cooperativa.votacao.api.voto;

import com.cooperativa.votacao.api.voto.dto.ResultadoResponse;
import com.cooperativa.votacao.api.voto.dto.VotoRequest;
import com.cooperativa.votacao.api.voto.dto.VotoResponse;
import com.cooperativa.votacao.application.voto.VotoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas/{id}")
public class VotoController {

    private final VotoService votoService;

    public VotoController(VotoService votoService) {
        this.votoService = votoService;
    }

    @PostMapping("/votos")
    public ResponseEntity<VotoResponse> votar(@PathVariable Long id, @RequestBody VotoRequest request) {
        var voto = votoService.registrar(id, request.associado(), request.voto());
        return ResponseEntity.status(HttpStatus.CREATED).body(VotoResponse.de(voto));
    }

    @GetMapping("/resultado")
    public ResponseEntity<ResultadoResponse> resultado(@PathVariable Long id) {
        var resultado = votoService.resultado(id);
        return ResponseEntity.ok(ResultadoResponse.de(resultado));
    }
}
