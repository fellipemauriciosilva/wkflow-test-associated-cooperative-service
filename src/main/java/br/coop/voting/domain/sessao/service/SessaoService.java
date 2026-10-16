package br.coop.voting.domain.sessao.service;

import br.coop.voting.domain.pauta.repository.PautaRepository;
import br.coop.voting.domain.sessao.dto.SessaoRequest;
import br.coop.voting.domain.sessao.dto.SessaoResponse;
import br.coop.voting.domain.sessao.entity.Sessao;
import br.coop.voting.domain.sessao.exception.SessaoAtivaException;
import br.coop.voting.domain.sessao.repository.SessaoRepository;
import br.coop.voting.shared.exception.PautaNaoEncontradaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@Slf4j
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final PautaRepository pautaRepository;

    public SessaoService(SessaoRepository sessaoRepository, PautaRepository pautaRepository) {
        this.sessaoRepository = sessaoRepository;
        this.pautaRepository = pautaRepository;
    }

    public SessaoResponse abrirSessao(Long pautaId, SessaoRequest request) {
        if (!pautaRepository.existsById(pautaId)) {
            throw new PautaNaoEncontradaException("Pauta com id " + pautaId + " não encontrada");
        }

        validarSessaoAtiva(pautaId);

        Sessao sessao = new Sessao();
        sessao.setPautaId(pautaId);
        sessao.setDurationMinutes(request.duracao_minutos());
        sessao.setAtiva(true);

        Sessao saved = sessaoRepository.save(sessao);
        log.info("Abrindo sessão para pauta {} com duração {} minutos", pautaId, request.duracao_minutos());

        return mapToResponse(saved);
    }

    public void validarSessaoAtiva(Long pautaId) {
        sessaoRepository.findByPautaIdAndAtivaTrue(pautaId).ifPresent(sessao -> {
            log.warn("Conflito: pauta {} já possui sessão ativa", pautaId);
            throw new SessaoAtivaException(pautaId);
        });
    }

    public void verificarExpiracoes() {
        List<Sessao> sessoes = sessaoRepository.findByAtivaAndDataFechamentoNull();
        LocalDateTime agora = LocalDateTime.now();

        sessoes.stream()
            .filter(s -> agora.isAfter(s.getDataAbertura().plusMinutes(s.getDurationMinutes())))
            .forEach(this::fecharSessao);
    }

    private void fecharSessao(Sessao sessao) {
        sessao.setAtiva(false);
        sessao.setDataFechamento(LocalDateTime.now());
        sessaoRepository.save(sessao);
        log.info("Sessão {} expirada, fechando", sessao.getId());
    }

    private SessaoResponse mapToResponse(Sessao sessao) {
        return SessaoResponse.builder()
            .id(sessao.getId())
            .pauta_id(sessao.getPautaId())
            .duracao_minutos(sessao.getDurationMinutes())
            .data_abertura(sessao.getDataAbertura())
            .data_fechamento(sessao.getDataFechamento())
            .ativa(sessao.getAtiva())
            .build();
    }
}
