package pautas;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas")
public class PautaController {

    private final PautaService service;

    public PautaController(PautaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PautaCriadaResponse> cadastrar(@Valid @RequestBody CriarPautaRequest request) {
        Long id = service.cadastrar(request);
        return ResponseEntity.created(URI.create("/pautas/" + id)).body(new PautaCriadaResponse(id));
    }

    @GetMapping("/{id}")
    public PautaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }
}
