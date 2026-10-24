package com.acme.pautas.web;

import com.acme.pautas.service.SessaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas")
public class SessaoController {

    private static final int DURACAO_PADRAO_MINUTOS = 1;

    private final SessaoService service;

    public SessaoController(SessaoService service) {
        this.service = service;
    }

    @PostMapping("/{id}/sessao")
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoResponse abrir(@PathVariable Long id,
                                @Valid @RequestBody(required = false) SessaoRequest request) {
        int duracao = request == null || request.duracaoMinutos() == null
                ? DURACAO_PADRAO_MINUTOS
                : request.duracaoMinutos();
        return SessaoResponse.de(service.abrir(id, duracao));
    }
}
