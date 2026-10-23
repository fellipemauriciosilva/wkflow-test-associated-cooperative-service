package br.coop.cooperative.pauta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/pautas")
public class PautaController {

    private final PautaRepository repository;

    public PautaController(PautaRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> criar(@Valid @RequestBody CriarPautaRequest request) {
        Pauta pauta = repository.save(new Pauta(request.titulo(), request.descricao()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", pauta.getId()));
    }

    @GetMapping("/{id}")
    public PautaResponse buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(PautaResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pauta não encontrada"));
    }

    public record CriarPautaRequest(
            @NotBlank @Size(max = 200) String titulo,
            @Size(max = 1000) String descricao) {
    }

    public record PautaResponse(Long id, String titulo, String descricao) {

        static PautaResponse from(Pauta pauta) {
            return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao());
        }
    }
}
