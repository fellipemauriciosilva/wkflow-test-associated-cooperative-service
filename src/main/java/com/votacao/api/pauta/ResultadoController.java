package com.votacao.api.pauta;

import com.votacao.api.pauta.dto.ResultadoResponse;
import com.votacao.application.pauta.ResultadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas/{id}/resultado")
public class ResultadoController {

    private final ResultadoService resultadoService;

    public ResultadoController(ResultadoService resultadoService) {
        this.resultadoService = resultadoService;
    }

    @GetMapping
    public ResponseEntity<ResultadoResponse> apurar(@PathVariable Long id) {
        return ResponseEntity.ok(resultadoService.apurar(id));
    }
}
