package br.coop.assembly.controller;

import br.coop.assembly.dto.ResultadoDTO;
import br.coop.assembly.dto.VotoDTO;
import br.coop.assembly.dto.VotoRequest;
import br.coop.assembly.service.VotoService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/pautas")
public class VotoController {
    private final VotoService votoService;

    public VotoController(VotoService votoService) {
        this.votoService = votoService;
    }

    @PostMapping("/{id}/votos")
    public ResponseEntity<VotoDTO> registrarVoto(
            @PathVariable Long id,
            @Valid @RequestBody VotoRequest request) {
        log.debug("Registrando voto para pauta: {}", id);
        VotoDTO votoDTO = votoService.registrarVoto(id, request.getAssociadoId(), request.getVoto());
        return ResponseEntity.status(HttpStatus.CREATED).body(votoDTO);
    }

    @GetMapping("/{id}/resultado")
    public ResponseEntity<ResultadoDTO> obterResultado(@PathVariable Long id) {
        log.debug("Obtendo resultado para pauta: {}", id);
        ResultadoDTO resultado = votoService.getResultado(id);
        return ResponseEntity.ok(resultado);
    }
}
