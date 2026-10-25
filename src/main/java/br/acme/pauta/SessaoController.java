package br.acme.pauta;

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

    private final SessaoService service;

    public SessaoController(SessaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SessaoResponse> abrir(@PathVariable Long id,
                                                @Valid @RequestBody(required = false) SessaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.abrir(id, request));
    }
}
