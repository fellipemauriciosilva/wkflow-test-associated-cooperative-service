package pautas;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas")
public class SessaoController {

    private final SessaoService service;

    public SessaoController(SessaoService service) {
        this.service = service;
    }

    @PostMapping("/{id}/sessao")
    public ResponseEntity<SessaoResponse> abrir(
            @PathVariable Long id, @Valid @RequestBody(required = false) AbrirSessaoRequest request) {
        Integer duracao = request == null ? null : request.duracaoMinutos();
        SessaoResponse sessao = service.abrir(id, duracao);
        return ResponseEntity.created(URI.create("/pautas/" + id + "/sessao")).body(sessao);
    }
}
