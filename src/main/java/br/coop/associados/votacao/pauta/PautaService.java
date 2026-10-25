package br.coop.associados.votacao.pauta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PautaService {

    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PautaResponse cadastrar(PautaRequest request) {
        Pauta pauta = repository.save(new Pauta(request.titulo(), request.descricao()));
        return PautaResponse.de(pauta);
    }

    public PautaResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(PautaResponse::de)
                .orElseThrow(() -> new PautaNaoEncontradaException(id));
    }
}
