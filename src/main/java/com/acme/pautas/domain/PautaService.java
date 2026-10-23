package com.acme.pautas.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PautaService {

    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    public Pauta criar(String titulo, String descricao) {
        return repository.save(Pauta.criar(titulo, descricao));
    }

    @Transactional(readOnly = true)
    public Pauta buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new PautaNaoEncontradaException(id));
    }
}
