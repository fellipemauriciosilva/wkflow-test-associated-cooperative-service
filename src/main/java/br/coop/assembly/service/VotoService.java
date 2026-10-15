package br.coop.assembly.service;

import br.coop.assembly.dto.ResultadoDTO;
import br.coop.assembly.entity.Sessao;
import br.coop.assembly.entity.Voto;
import br.coop.assembly.entity.Voto.VotoEnum;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.SessaoNaoAtivaException;
import br.coop.assembly.exception.VotoDuplicadoException;
import br.coop.assembly.repository.PautaRepository;
import br.coop.assembly.repository.SessaoRepository;
import br.coop.assembly.repository.VotoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class VotoService {
    private final VotoRepository votoRepository;
    private final PautaRepository pautaRepository;
    private final SessaoRepository sessaoRepository;
    private final Clock clock;

    public VotoService(VotoRepository votoRepository, PautaRepository pautaRepository,
                       SessaoRepository sessaoRepository, Clock clock) {
        this.votoRepository = votoRepository;
        this.pautaRepository = pautaRepository;
        this.sessaoRepository = sessaoRepository;
        this.clock = clock;
    }

    @Transactional
    public Voto registrarVoto(Long pautaId, String associadoId, VotoEnum voto) {
        log.debug("Registrando voto para pauta: {}, associado: {}", pautaId, associadoId);

        pautaRepository.findById(pautaId)
                .orElseThrow(() -> new PautaNotFoundException(pautaId));

        if (associadoId == null || associadoId.isBlank()) {
            throw new IllegalArgumentException("AssociadoId obrigatório");
        }

        if (voto == null) {
            throw new IllegalArgumentException("Voto inválido. Valores aceitos: SIM, NAO");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Optional<Sessao> sessao = sessaoRepository.findActiveSessaoForPauta(pautaId, now);
        if (sessao.isEmpty()) {
            throw new SessaoNaoAtivaException("Sessão não está ativa para votação");
        }

        votoRepository.findByPautaIdAndAssociadoId(pautaId, associadoId)
                .ifPresent(v -> {
                    throw new VotoDuplicadoException("Este associado já votou nesta pauta");
                });

        Voto novoVoto = new Voto();
        novoVoto.setPautaId(pautaId);
        novoVoto.setAssociadoId(associadoId);
        novoVoto.setVoto(voto);

        return votoRepository.save(novoVoto);
    }

    @Transactional(readOnly = true)
    public ResultadoDTO getResultado(Long pautaId) {
        log.debug("Recuperando resultado para pauta: {}", pautaId);

        pautaRepository.findById(pautaId)
                .orElseThrow(() -> new PautaNotFoundException(pautaId));

        long votosSim = votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.SIM);
        long votosNao = votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.NAO);
        long totalVotos = votosSim + votosNao;

        BigDecimal percentualSim = BigDecimal.ZERO;
        BigDecimal percentualNao = BigDecimal.ZERO;

        if (totalVotos > 0) {
            percentualSim = BigDecimal.valueOf(votosSim)
                    .divide(BigDecimal.valueOf(totalVotos), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            percentualNao = BigDecimal.valueOf(votosNao)
                    .divide(BigDecimal.valueOf(totalVotos), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Optional<Sessao> sessao = sessaoRepository.findByPautaId(pautaId);
        String status;
        if (sessao.isEmpty()) {
            status = "SEM_SESSAO";
        } else {
            status = now.isAfter(sessao.get().getDataFim()) ? "EXPIRADA" : "ATIVA";
        }

        return ResultadoDTO.builder()
                .pautaId(pautaId)
                .totalVotos(totalVotos)
                .votosSim(votosSim)
                .votosNao(votosNao)
                .percentualSim(percentualSim)
                .percentualNao(percentualNao)
                .status(status)
                .build();
    }
}
