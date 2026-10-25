package com.acme.pautas.web;

import com.acme.pautas.service.VotoService;
import jakarta.validation.Valid;
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

    public VotoController(VotoService service) {
        this.service = service;
    }

    @PostMapping("/votos")
    @ResponseStatus(HttpStatus.CREATED)
    public VotoResponse votar(@PathVariable Long id, @Valid @RequestBody VotoRequest request) {
        return VotoResponse.de(service.registrar(id, request.associadoId(), request.voto()));
    }

    @GetMapping("/resultado")
    public ResultadoResponse resultado(@PathVariable Long id) {
        return ResultadoResponse.de(service.resultado(id));
    }
}
