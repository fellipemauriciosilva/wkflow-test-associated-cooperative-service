package com.votacao.application.sessao;

import com.votacao.application.pauta.PautaService;
import com.votacao.domain.pauta.Pauta;
import com.votacao.domain.sessao.Sessao;
import com.votacao.domain.sessao.SessaoJaAbertaException;
import com.votacao.domain.sessao.SessaoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final PautaService pautaService;
    private final Clock clock;

    public SessaoService(SessaoRepository sessaoRepository, PautaService pautaService, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.pautaService = pautaService;
        this.clock = clock;
    }

    public Sessao abrir(Long pautaId, int duracaoMinutos) {
        Pauta pauta = pautaService.buscarPorId(pautaId);

        if (sessaoRepository.findByPautaId(pautaId).isPresent()) {
            throw new SessaoJaAbertaException(pautaId);
        }

        LocalDateTime abertura = LocalDateTime.now(clock);
        Sessao sessao = new Sessao(pauta, abertura, duracaoMinutos);

        try {
            return sessaoRepository.save(sessao);
        } catch (DataIntegrityViolationException ex) {
            throw new SessaoJaAbertaException(pautaId);
        }
    }
}
