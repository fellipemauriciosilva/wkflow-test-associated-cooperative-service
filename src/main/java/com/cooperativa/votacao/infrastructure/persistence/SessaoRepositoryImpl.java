package com.cooperativa.votacao.infrastructure.persistence;

import com.cooperativa.votacao.domain.sessao.Sessao;
import com.cooperativa.votacao.domain.sessao.SessaoRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SessaoRepositoryImpl implements SessaoRepository {

    private final SessaoJpaRepository jpaRepository;

    public SessaoRepositoryImpl(SessaoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Sessao salvar(Sessao sessao) {
        return jpaRepository.save(sessao);
    }

    @Override
    public List<Sessao> buscarPorPautaId(Long pautaId) {
        return jpaRepository.findByPautaId(pautaId);
    }
}
