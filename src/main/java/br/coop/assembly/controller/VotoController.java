package br.coop.assembly.controller;

import br.coop.assembly.dto.RegistrarVotoRequest;
import br.coop.assembly.dto.ResultadoDTO;
import br.coop.assembly.dto.VotoResponse;
import br.coop.assembly.entity.Voto;
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
    public ResponseEntity<VotoResponse> registrarVoto(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarVotoRequest request) {
        log.debug("POST /pautas/{}/votos", id);

        Voto voto = votoService.registrarVoto(id, request.getAssociadoId(), request.getVoto());

        VotoResponse response = VotoResponse.builder()
                .id(voto.getId())
                .pautaId(voto.getPautaId())
                .associadoId(voto.getAssociadoId())
                .voto(voto.getVoto())
                .timestamp(voto.getTimestamp())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/resultado")
    public ResponseEntity<ResultadoDTO> getResultado(@PathVariable Long id) {
        log.debug("GET /pautas/{}/resultado", id);

        ResultadoDTO resultado = votoService.getResultado(id);

        return ResponseEntity.ok(resultado);
    }
}
