package com.cooperativa.votacao.application.pauta;

import com.cooperativa.votacao.domain.pauta.Pauta;
import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import com.cooperativa.votacao.domain.pauta.PautaRepository;
import org.springframework.stereotype.Service;

@Service
public class PautaService {

    private final PautaRepository pautaRepository;

    public PautaService(PautaRepository pautaRepository) {
        this.pautaRepository = pautaRepository;
    }

    public Pauta criar(String titulo, String descricao) {
        return pautaRepository.salvar(new Pauta(titulo, descricao));
    }

    public Pauta buscarPorId(Long id) {
        return pautaRepository.buscarPorId(id)
                .orElseThrow(() -> new PautaNaoEncontradaException(id));
    }
}
