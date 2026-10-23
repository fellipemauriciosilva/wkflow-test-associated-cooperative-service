package br.coop.acme.pauta.service;

import br.coop.acme.pauta.entity.Pauta;
import br.coop.acme.pauta.repository.PautaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PautaService {

    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    public Pauta cadastrar(String titulo, String descricao) {
        return repository.save(new Pauta(titulo, descricao));
    }

    public Optional<Pauta> buscarPorId(Long id) {
        return repository.findById(id);
    }
}
