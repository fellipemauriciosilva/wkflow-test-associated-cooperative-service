package com.cooperativa.votacao.api.sessao;

import com.cooperativa.votacao.api.sessao.dto.SessaoRequest;
import com.cooperativa.votacao.api.sessao.dto.SessaoResponse;
import com.cooperativa.votacao.application.sessao.SessaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas/{id}/sessao")
public class SessaoController {

    private final SessaoService sessaoService;

    public SessaoController(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @PostMapping
    public ResponseEntity<SessaoResponse> abrir(
            @PathVariable Long id, @RequestBody(required = false) SessaoRequest request) {
        var duracaoMinutos = request == null ? null : request.duracaoMinutos();
        var sessao = sessaoService.abrir(id, duracaoMinutos);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessaoResponse.de(sessao));
    }
}
