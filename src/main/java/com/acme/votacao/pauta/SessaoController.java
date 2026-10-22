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
public class SessaoController {

    private final SessaoService sessaoService;

    public SessaoController(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @PostMapping("/{id}/sessao")
    public ResponseEntity<SessaoResponse> abrir(@PathVariable Long id,
                                                 @Valid @RequestBody(required = false) SessaoRequest request) {
        SessaoRequest requestOuPadrao = request != null ? request : new SessaoRequest(null);
        Sessao sessao = sessaoService.abrir(id, requestOuPadrao);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessaoResponse.from(sessao));
    }
}
