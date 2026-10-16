package br.coop.voting.domain.voto.service;

import br.coop.voting.domain.pauta.repository.PautaRepository;
import br.coop.voting.domain.sessao.repository.SessaoRepository;
import br.coop.voting.domain.voto.dto.VotoRequestDTO;
import br.coop.voting.domain.voto.dto.VotoResponseDTO;
import br.coop.voting.domain.voto.dto.VotoResultadoDTO;
import br.coop.voting.domain.voto.entity.Voto;
import br.coop.voting.domain.voto.entity.VotoEnum;
import br.coop.voting.domain.voto.exception.VotacaoFechadaException;
import br.coop.voting.domain.voto.exception.VotoDuplicadoException;
import br.coop.voting.domain.voto.repository.VotoRepository;
import br.coop.voting.shared.exception.PautaNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class VotoService {

    private final VotoRepository votoRepository;
    private final PautaRepository pautaRepository;
    private final SessaoRepository sessaoRepository;

    public VotoResponseDTO registrarVoto(Long pautaId, VotoRequestDTO request) {
        pautaRepository.findById(pautaId)
            .orElseThrow(() -> new PautaNaoEncontradaException("Pauta " + pautaId + " não encontrada"));

        var sessao = sessaoRepository.findByPautaIdAndAtivaTrue(pautaId)
            .orElseThrow(() -> new VotacaoFechadaException(pautaId));

        votoRepository.findByPautaIdAndIdAssociado(pautaId, request.idAssociado())
            .ifPresent(v -> {
                throw new VotoDuplicadoException(pautaId, request.idAssociado());
            });

        Voto voto = new Voto();
        voto.setPautaId(pautaId);
        voto.setSessaoId(sessao.getId());
        voto.setIdAssociado(request.idAssociado());
        voto.setVoto(VotoEnum.valueOf(request.voto().toUpperCase()));

        Voto saved = votoRepository.save(voto);
        log.info("Voto registrado: pauta={}, associado={}, voto={}", pautaId, request.idAssociado(), request.voto());

        return toResponseDTO(saved);
    }

    public VotoResultadoDTO consultarResultado(Long pautaId) {
        pautaRepository.findById(pautaId)
            .orElseThrow(() -> new PautaNaoEncontradaException("Pauta " + pautaId + " não encontrada"));

        Long sim = votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.SIM);
        Long nao = votoRepository.countByPautaIdAndVoto(pautaId, VotoEnum.NAO);

        return new VotoResultadoDTO(pautaId, sim, nao);
    }

    private VotoResponseDTO toResponseDTO(Voto voto) {
        return new VotoResponseDTO(
            voto.getId(),
            voto.getPautaId(),
            voto.getIdAssociado(),
            voto.getVoto().toString(),
            voto.getCreatedAt()
        );
    }
}
