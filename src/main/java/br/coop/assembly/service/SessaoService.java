package br.coop.assembly.service;

import br.coop.assembly.entity.Pauta;
import br.coop.assembly.entity.Sessao;
import br.coop.assembly.exception.DuracaoInvalidaException;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.SessaoJaAbertaException;
import br.coop.assembly.repository.PautaRepository;
import br.coop.assembly.repository.SessaoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
public class SessaoService {
    private final SessaoRepository sessaoRepository;
    private final PautaRepository pautaRepository;
    private final Clock clock;

    @Value("${sessao.duracao.padrao.minutos:1}")
    private int duracaoPadraoMinutos;

    @Value("${sessao.duracao.minima.minutos:1}")
    private int duracaoMinimaMinutos;

    @Value("${sessao.duracao.maxima.minutos:1440}")
    private int duracaoMaximaMinutos;

    public SessaoService(SessaoRepository sessaoRepository, PautaRepository pautaRepository, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.pautaRepository = pautaRepository;
        this.clock = clock;
    }

    @Transactional
    public Sessao abrirSessao(Long pautaId, Integer duracaoMinutos) {
        log.debug("Abrindo sessao para pauta: {}", pautaId);

        Pauta pauta = pautaRepository.findById(pautaId)
                .orElseThrow(() -> new PautaNotFoundException(pautaId));

        Integer duracao = duracaoMinutos != null ? duracaoMinutos : duracaoPadraoMinutos;
        validateDuracao(duracao);

        LocalDateTime now = LocalDateTime.now(clock);
        sessaoRepository.findActiveSessaoForPauta(pautaId, now)
                .ifPresent(s -> {
                    throw new SessaoJaAbertaException(pautaId);
                });

        Sessao sessao = new Sessao();
        sessao.setPauta(pauta);
        sessao.setPautaId(pautaId);
        sessao.setDataInicio(now);
        sessao.setDataFim(calculateDataFim(now, duracao));
        sessao.setStatus(Sessao.Status.ATIVA);

        Sessao saved = sessaoRepository.save(sessao);
        log.debug("Sessao aberta com id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Sessao abrirSessao(Long pautaId) {
        return abrirSessao(pautaId, null);
    }

    @Transactional(readOnly = true)
    public boolean isSessionActive(Sessao sessao) {
        LocalDateTime now = LocalDateTime.now(clock);
        return now.isBefore(sessao.getDataFim());
    }

    @Transactional(readOnly = true)
    public Sessao.Status getSessionStatus(Sessao sessao) {
        return isSessionActive(sessao) ? Sessao.Status.ATIVA : Sessao.Status.EXPIRADA;
    }

    @Transactional(readOnly = true)
    public boolean canAcceptVotes(Long sessaoId) {
        return sessaoRepository.findById(sessaoId)
                .map(this::isSessionActive)
                .orElse(false);
    }

    public LocalDateTime calculateDataFim(LocalDateTime dataInicio, int duracaoMinutos) {
        return dataInicio.plusMinutes(duracaoMinutos);
    }

    private void validateDuracao(Integer duracao) {
        if (duracao == null || duracao <= 0) {
            throw new DuracaoInvalidaException("Duracao deve ser > 0");
        }
        if (duracao > duracaoMaximaMinutos) {
            throw new DuracaoInvalidaException("Duracao maxima e 1440 minutos");
        }
    }
}
