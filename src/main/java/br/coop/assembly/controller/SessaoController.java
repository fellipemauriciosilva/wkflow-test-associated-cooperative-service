package br.coop.assembly.controller;

import br.coop.assembly.dto.AbrirSessaoRequest;
import br.coop.assembly.dto.SessaoResponse;
import br.coop.assembly.entity.Sessao;
import br.coop.assembly.service.SessaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/pautas")
public class SessaoController {
    private final SessaoService service;

    public SessaoController(SessaoService service) {
        this.service = service;
    }

    @PostMapping("/{pautaId}/sessao")
    public ResponseEntity<SessaoResponse> abrirSessao(
            @PathVariable Long pautaId,
            @RequestBody(required = false) AbrirSessaoRequest request) {
        log.debug("Abrindo sessao para pauta: {}", pautaId);

        Integer duracaoMinutos = request != null ? request.getDuracaoMinutos() : null;
        Sessao sessao = service.abrirSessao(pautaId, duracaoMinutos);
        SessaoResponse response = SessaoResponse.from(sessao);

        return ResponseEntity
                .created(URI.create("/pautas/" + pautaId + "/sessao/" + sessao.getId()))
                .body(response);
    }
}
