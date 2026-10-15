package br.coop.assembly.service;

import br.coop.assembly.dto.ResultadoDTO;
import br.coop.assembly.dto.VotoDTO;
import br.coop.assembly.entity.Pauta;
import br.coop.assembly.entity.Sessao;
import br.coop.assembly.entity.Voto;
import br.coop.assembly.exception.*;
import br.coop.assembly.repository.PautaRepository;
import br.coop.assembly.repository.SessaoRepository;
import br.coop.assembly.repository.VotoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class VotoService {
    private final VotoRepository votoRepository;
    private final SessaoRepository sessaoRepository;
    private final PautaRepository pautaRepository;

    public VotoService(VotoRepository votoRepository, SessaoRepository sessaoRepository, PautaRepository pautaRepository) {
        this.votoRepository = votoRepository;
        this.sessaoRepository = sessaoRepository;
        this.pautaRepository = pautaRepository;
    }

    @Transactional
    public VotoDTO registrarVoto(Long pautaId, String associadoId, String votoStr) {
        log.debug("Registrando voto");

        if (associadoId == null || associadoId.trim().isEmpty()) {
            throw new AssociadoInvalidoException("Associado invalido");
        }

        try {
            pautaRepository.getById(pautaId);
        } catch (Exception e) {
            throw new PautaNotFoundException("Pauta nao encontrada");
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<Sessao> sessaoOpt = sessaoRepository.findActiveSessaoForPauta(pautaId, now);
        if (!sessaoOpt.isPresent()) {
            throw new SessaoNaoAtivaException("Sessao nao esta ativa");
        }

        Voto.VotoEnum votoEnum;
        try {
            votoEnum = Voto.VotoEnum.valueOf(votoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new VotoInvalidoException("Voto invalido");
        }

        Optional<Voto> existente = votoRepository.findByPautaIdAndAssociadoId(pautaId, associadoId);
        if (existente.isPresent()) {
            throw new VotoDuplicadoException("Voto duplicado");
        }

        Pauta pauta = sessaoOpt.get().getPauta();
        Voto voto = new Voto();
        voto.setPauta(pauta);
        voto.setAssociadoId(associadoId);
        voto.setVoto(votoEnum);
        Voto saved = votoRepository.save(voto);
        return VotoDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ResultadoDTO getResultado(Long pautaId) {
        log.debug("Obtendo resultado");

        try {
            pautaRepository.getById(pautaId);
        } catch (Exception e) {
            throw new PautaNotFoundException("Pauta nao encontrada");
        }

        List<Voto> votos = votoRepository.findByPautaId(pautaId);
        int sim = (int) votos.stream().filter(v -> v.getVoto() == Voto.VotoEnum.SIM).count();
        int nao = (int) votos.stream().filter(v -> v.getVoto() == Voto.VotoEnum.NAO).count();

        LocalDateTime now = LocalDateTime.now();
        String status = "SEM_SESSAO";

        if (sessaoRepository.findActiveSessaoForPauta(pautaId, now).isPresent()) {
            status = "ATIVA";
        } else if (sessaoRepository.findByPautaId(pautaId).isPresent()) {
            status = "EXPIRADA";
        }

        return ResultadoDTO.calcular(sim, nao, status);
    }
}
