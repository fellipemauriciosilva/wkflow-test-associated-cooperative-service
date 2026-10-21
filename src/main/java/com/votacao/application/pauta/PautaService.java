package com.votacao.application.pauta;

import com.votacao.domain.pauta.Pauta;
import com.votacao.domain.pauta.PautaNotFoundException;
import com.votacao.domain.pauta.PautaRepository;
import org.springframework.stereotype.Service;

@Service
public class PautaService {

    private final PautaRepository pautaRepository;

    public PautaService(PautaRepository pautaRepository) {
        this.pautaRepository = pautaRepository;
    }

    public Pauta criar(String titulo, String descricao) {
        Pauta pauta = new Pauta(titulo, descricao);
        return pautaRepository.save(pauta);
    }

    public Pauta buscarPorId(Long id) {
        return pautaRepository.findById(id)
                .orElseThrow(() -> new PautaNotFoundException(id));
    }
}
