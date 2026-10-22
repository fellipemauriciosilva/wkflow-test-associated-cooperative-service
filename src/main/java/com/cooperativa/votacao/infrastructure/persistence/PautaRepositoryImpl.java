package com.cooperativa.votacao.infrastructure.persistence;

import com.cooperativa.votacao.domain.pauta.Pauta;
import com.cooperativa.votacao.domain.pauta.PautaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PautaRepositoryImpl implements PautaRepository {

    private final PautaJpaRepository jpaRepository;

    public PautaRepositoryImpl(PautaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Pauta salvar(Pauta pauta) {
        return jpaRepository.save(pauta);
    }

    @Override
    public Optional<Pauta> buscarPorId(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Pauta> buscarPorIdComLock(Long id) {
        return jpaRepository.buscarPorIdComLock(id);
    }
}
