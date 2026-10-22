package com.cooperativa.votacao.application.sessao;

import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import com.cooperativa.votacao.domain.pauta.PautaRepository;
import com.cooperativa.votacao.domain.sessao.Sessao;
import com.cooperativa.votacao.domain.sessao.SessaoJaAbertaException;
import com.cooperativa.votacao.domain.sessao.SessaoRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class SessaoService {

    private final PautaRepository pautaRepository;
    private final SessaoRepository sessaoRepository;
    private final Clock clock;

    public SessaoService(PautaRepository pautaRepository, SessaoRepository sessaoRepository, Clock clock) {
        this.pautaRepository = pautaRepository;
        this.sessaoRepository = sessaoRepository;
        this.clock = clock;
    }

    public Sessao abrir(Long pautaId, Integer duracaoMinutos) {
        var duracaoResolvida = duracaoMinutos == null ? Sessao.DURACAO_PADRAO_MINUTOS : duracaoMinutos;
        Sessao.validarDuracao(duracaoResolvida);

        var pauta = pautaRepository.buscarPorId(pautaId)
                .orElseThrow(() -> new PautaNaoEncontradaException(pautaId));

        var agora = clock.instant();
        var existeSessaoAberta = sessaoRepository.buscarPorPautaId(pautaId).stream()
                .anyMatch(sessao -> sessao.estaAberta(agora));
        if (existeSessaoAberta) {
            throw new SessaoJaAbertaException(pautaId);
        }

        return sessaoRepository.salvar(new Sessao(pauta, duracaoResolvida, agora));
    }
}
