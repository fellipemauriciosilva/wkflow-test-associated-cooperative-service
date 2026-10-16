package br.coop.voting.domain.pauta.service;

import br.coop.voting.domain.pauta.dto.AtualizarPautaRequest;
import br.coop.voting.domain.pauta.dto.CriarPautaRequest;
import br.coop.voting.domain.pauta.dto.PautaResponse;
import br.coop.voting.domain.pauta.entity.Pauta;
import br.coop.voting.domain.pauta.repository.PautaRepository;
import br.coop.voting.domain.sessao.repository.SessaoRepository;
import br.coop.voting.shared.exception.PautaNaoEncontradaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class PautaService {

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    public PautaResponse criar(CriarPautaRequest request) {
        Pauta pauta = new Pauta();
        pauta.setTitulo(request.getTitulo());
        pauta.setDescricao(request.getDescricao());
        Pauta salva = pautaRepository.save(pauta);
        log.info("Pauta criada: id={}, titulo={}", salva.getId(), salva.getTitulo());
        return toPautaResponse(salva);
    }

    @Transactional(readOnly = true)
    public PautaResponse obterPorId(Long id) {
        Pauta pauta = pautaRepository.findById(id)
            .orElseThrow(() -> new PautaNaoEncontradaException("Pauta com id " + id + " não encontrada"));
        return toPautaResponse(pauta);
    }

    @Transactional(readOnly = true)
    public List<PautaResponse> listarTodas() {
        return pautaRepository.findAll().stream()
            .map(this::toPautaResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PautaResponse> buscarPorTitulo(String titulo) {
        return pautaRepository.findByTituloContainingIgnoreCase(titulo).stream()
            .map(this::toPautaResponse)
            .toList();
    }

    public PautaResponse atualizar(Long id, AtualizarPautaRequest request) {
        Pauta pauta = pautaRepository.findById(id)
            .orElseThrow(() -> new PautaNaoEncontradaException("Pauta com id " + id + " não encontrada"));
        pauta.setTitulo(request.getTitulo());
        pauta.setDescricao(request.getDescricao());
        Pauta atualizada = pautaRepository.save(pauta);
        log.info("Pauta atualizada: id={}, titulo={}", atualizada.getId(), atualizada.getTitulo());
        return toPautaResponse(atualizada);
    }

    public void deletar(Long id) {
        Pauta pauta = pautaRepository.findById(id)
            .orElseThrow(() -> new PautaNaoEncontradaException("Pauta com id " + id + " não encontrada"));
        pautaRepository.delete(pauta);
        log.info("Pauta deletada: id={}", id);
    }

    private PautaResponse toPautaResponse(Pauta pauta) {
        var sessaoAtiva = sessaoRepository.findByPautaIdAndAtivaTrue(pauta.getId());
        String statusSessao = "PAUTA_SEM_SESSAO";
        br.coop.voting.domain.sessao.dto.SessaoResponse sessaoResponse = null;

        if (sessaoAtiva.isPresent()) {
            var sessao = sessaoAtiva.get();
            statusSessao = "SESSAO_ABERTA";
            sessaoResponse = br.coop.voting.domain.sessao.dto.SessaoResponse.builder()
                .id(sessao.getId())
                .pauta_id(sessao.getPautaId())
                .duracao_minutos(sessao.getDurationMinutes())
                .data_abertura(sessao.getDataAbertura())
                .data_fechamento(sessao.getDataFechamento())
                .ativa(sessao.getAtiva())
                .build();
        } else {
            var sessaoFechada = sessaoRepository.findLatestClosedByPautaId(pauta.getId());
            if (sessaoFechada.isPresent()) {
                statusSessao = "SESSAO_FECHADA";
            }
        }

        return PautaResponse.builder()
            .id(pauta.getId())
            .titulo(pauta.getTitulo())
            .descricao(pauta.getDescricao())
            .status_sessao(statusSessao)
            .sessao(sessaoResponse)
            .createdAt(pauta.getCreatedAt())
            .updatedAt(pauta.getUpdatedAt())
            .build();
    }
}
