package com.cooperativa.votacao.infrastructure.persistence;

import com.cooperativa.votacao.domain.voto.Voto;
import com.cooperativa.votacao.domain.voto.VotoRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class VotoRepositoryImpl implements VotoRepository {

    private final VotoJpaRepository jpaRepository;

    public VotoRepositoryImpl(VotoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Voto salvar(Voto voto) {
        return jpaRepository.saveAndFlush(voto);
    }

    @Override
    public List<Voto> buscarPorPautaId(Long pautaId) {
        return jpaRepository.findByPautaId(pautaId);
    }
}
