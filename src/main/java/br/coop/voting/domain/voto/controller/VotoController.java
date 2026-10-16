package br.coop.voting.domain.voto.controller;

import br.coop.voting.domain.voto.dto.VotoRequestDTO;
import br.coop.voting.domain.voto.dto.VotoResponseDTO;
import br.coop.voting.domain.voto.dto.VotoResultadoDTO;
import br.coop.voting.domain.voto.service.VotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/pautas")
@RequiredArgsConstructor
public class VotoController {

    private final VotoService votoService;

    @PostMapping("/{id}/votos")
    public ResponseEntity<VotoResponseDTO> registrarVoto(
        @PathVariable Long id,
        @Valid @RequestBody VotoRequestDTO request
    ) {
        VotoResponseDTO response = votoService.registrarVoto(id, request);
        return ResponseEntity
            .created(URI.create("/pautas/" + id + "/votos/" + response.id()))
            .body(response);
    }

    @GetMapping("/{id}/resultado")
    public ResponseEntity<VotoResultadoDTO> consultarResultado(@PathVariable Long id) {
        VotoResultadoDTO resultado = votoService.consultarResultado(id);
        return ResponseEntity.ok(resultado);
    }
}
