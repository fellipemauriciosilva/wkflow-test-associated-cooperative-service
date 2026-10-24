package com.acme.pautas.service;

import com.acme.pautas.domain.Pauta;
import com.acme.pautas.repository.PautaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PautaService {

    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pauta cadastrar(String titulo, String descricao) {
        return repository.save(new Pauta(titulo, descricao));
    }

    @Transactional(readOnly = true)
    public Pauta buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new PautaNaoEncontradaException(id));
    }
}
