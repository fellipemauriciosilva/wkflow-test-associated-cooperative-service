package com.acme.pautas.pauta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PautaService {

    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    public Long cadastrar(String titulo, String descricao) {
        return repository.save(new Pauta(titulo, descricao)).getId();
    }

    @Transactional(readOnly = true)
    public Pauta buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new PautaNaoEncontradaException(id));
    }
}
