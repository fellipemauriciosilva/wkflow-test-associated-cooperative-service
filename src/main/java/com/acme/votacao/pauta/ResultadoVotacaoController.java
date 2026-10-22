package com.acme.votacao.pauta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas")
public class ResultadoVotacaoController {

    private final ResultadoVotacaoService resultadoVotacaoService;

    public ResultadoVotacaoController(ResultadoVotacaoService resultadoVotacaoService) {
        this.resultadoVotacaoService = resultadoVotacaoService;
    }

    @GetMapping("/{id}/resultado")
    public ResultadoVotacaoResponse resultado(@PathVariable Long id) {
        return resultadoVotacaoService.calcular(id);
    }
}
