package com.acme.pautas.web;

import com.acme.pautas.service.VotacaoIndisponivelException;
import com.acme.pautas.service.VotoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas/{id}")
public class VotoController {

    private final VotoService service;
    private final boolean votosHabilitados;

    public VotoController(VotoService service,
                          @Value("${pautas.votos.habilitado:false}") boolean votosHabilitados) {
        this.service = service;
        this.votosHabilitados = votosHabilitados;
    }

    @PostMapping("/votos")
    @ResponseStatus(HttpStatus.CREATED)
    public VotoResponse votar(@PathVariable Long id, @Valid @RequestBody VotoRequest request) {
        if (!votosHabilitados) {
            throw new VotacaoIndisponivelException();
        }
        return VotoResponse.de(service.registrar(id, request.associadoId(), request.voto()));
    }

    @GetMapping("/resultado")
    public ResultadoResponse resultado(@PathVariable Long id) {
        return ResultadoResponse.de(service.resultado(id));
    }
}
