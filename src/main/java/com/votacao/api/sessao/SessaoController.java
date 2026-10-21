package com.votacao.api.sessao;

import com.votacao.api.sessao.dto.SessaoRequest;
import com.votacao.api.sessao.dto.SessaoResponse;
import com.votacao.application.sessao.SessaoService;
import com.votacao.domain.sessao.Sessao;
import jakarta.validation.Valid;
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
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SessaoRequest request) {
        SessaoRequest efetiva = request != null ? request : new SessaoRequest(null);
        Sessao sessao = sessaoService.abrir(id, efetiva.duracaoMinutosOuPadrao());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessaoResponse.from(sessao));
    }
}
