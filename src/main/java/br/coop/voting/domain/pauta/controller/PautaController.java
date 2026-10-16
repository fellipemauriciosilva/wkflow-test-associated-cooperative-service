package br.coop.voting.domain.pauta.controller;

import br.coop.voting.domain.pauta.dto.AtualizarPautaRequest;
import br.coop.voting.domain.pauta.dto.CriarPautaRequest;
import br.coop.voting.domain.pauta.dto.PautaResponse;
import br.coop.voting.domain.pauta.service.PautaService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/pautas")
@Slf4j
public class PautaController {

    @Autowired
    private PautaService pautaService;

    @PostMapping
    public ResponseEntity<PautaResponse> criar(@Valid @RequestBody CriarPautaRequest request) {
        log.debug("Criar pauta: {}", request);
        PautaResponse response = pautaService.criar(request);
        return ResponseEntity.created(
            URI.create("/pautas/" + response.getId())
        ).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PautaResponse> obterPorId(@PathVariable Long id) {
        log.debug("Obter pauta por id: {}", id);
        PautaResponse response = pautaService.obterPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PautaResponse>> listarTodas(@RequestParam(required = false) String titulo) {
        log.debug("Listar pautas: titulo={}", titulo);
        List<PautaResponse> response;
        if (titulo != null && !titulo.isBlank()) {
            response = pautaService.buscarPorTitulo(titulo);
        } else {
            response = pautaService.listarTodas();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PautaResponse> atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarPautaRequest request) {
        log.debug("Atualizar pauta: id={}, request={}", id, request);
        PautaResponse response = pautaService.atualizar(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        log.debug("Deletar pauta: id={}", id);
        pautaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
