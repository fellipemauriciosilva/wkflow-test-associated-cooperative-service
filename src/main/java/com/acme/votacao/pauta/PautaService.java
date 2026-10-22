package com.acme.votacao.pauta;

import org.springframework.stereotype.Service;

@Service
public class PautaService {

    private final PautaRepository pautaRepository;

    public PautaService(PautaRepository pautaRepository) {
        this.pautaRepository = pautaRepository;
    }

    public Pauta criar(PautaRequest request) {
        Pauta pauta = new Pauta(request.titulo(), request.descricao());
        return pautaRepository.save(pauta);
    }

    public Pauta buscarPorId(Long id) {
        return pautaRepository.findById(id)
                .orElseThrow(() -> new PautaNaoEncontradaException(id));
    }
}
