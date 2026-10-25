package br.acme.pauta;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VotoController {

    private final VotoService service;

    public VotoController(VotoService service) {
        this.service = service;
    }

    @PostMapping("/pautas/{id}/votos")
    public ResponseEntity<VotoResponse> registrar(@PathVariable Long id, @Valid @RequestBody VotoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(id, request));
    }

    @GetMapping("/pautas/{id}/resultado")
    public ResultadoResponse resultado(@PathVariable Long id) {
        return service.resultado(id);
    }
}
