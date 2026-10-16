package br.coop.voting.domain.sessao.controller;

import br.coop.voting.domain.sessao.dto.SessaoRequest;
import br.coop.voting.domain.sessao.dto.SessaoResponse;
import br.coop.voting.domain.sessao.service.SessaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/pautas/{pautaId}/sessao")
public class SessaoController {

    private final SessaoService sessaoService;

    public SessaoController(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @PostMapping
    public ResponseEntity<SessaoResponse> abrirSessao(
        @PathVariable Long pautaId,
        @RequestBody @Valid SessaoRequest request
    ) {
        SessaoResponse response = sessaoService.abrirSessao(pautaId, request);
        URI location = URI.create("/pautas/" + pautaId + "/sessao/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }
}
