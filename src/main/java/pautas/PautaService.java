package pautas;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PautaService {

    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Long cadastrar(CriarPautaRequest request) {
        Pauta pauta = repository.save(new Pauta(request.titulo(), request.descricao()));
        return pauta.getId();
    }

    @Transactional(readOnly = true)
    public PautaResponse buscar(Long id) {
        return repository.findById(id)
                .map(PautaResponse::de)
                .orElseThrow(() -> new PautaNaoEncontradaException(id));
    }
}
