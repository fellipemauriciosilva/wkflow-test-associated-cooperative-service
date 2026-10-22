package com.cooperativa.votacao.application.sessao;

import com.cooperativa.votacao.domain.pauta.PautaNaoEncontradaException;
import com.cooperativa.votacao.domain.pauta.PautaRepository;
import com.cooperativa.votacao.domain.sessao.Sessao;
import com.cooperativa.votacao.domain.sessao.SessaoJaAbertaException;
import com.cooperativa.votacao.domain.sessao.SessaoRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Ordem 400 (duracao) -> 404 (pauta) -> 409 (sessao aberta) preservada. O lock
     * PESSIMISTIC_WRITE em Pauta (dentro desta transacao) serializa requisicoes concorrentes
     * para a MESMA pauta: a segunda requisicao bloqueia no buscarPorIdComLock ate a primeira
     * commitar, entao sempre ve a sessao recem-criada antes de decidir 409. Decisao (vs.
     * @Version otimista): pessimista evita retry/exception-translation para o caller e e
     * mais simples de raciocinar aqui, ao custo de uma linha de lock por request — aceitavel
     * dado volume baixo esperado de aberturas de sessao. Ver docs/CONTEXT.md.
     */
    @Transactional
    public Sessao abrir(Long pautaId, Integer duracaoMinutos) {
        var duracaoResolvida = duracaoMinutos == null ? Sessao.DURACAO_PADRAO_MINUTOS : duracaoMinutos;
        Sessao.validarDuracao(duracaoResolvida);

        var pauta = pautaRepository.buscarPorIdComLock(pautaId)
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
