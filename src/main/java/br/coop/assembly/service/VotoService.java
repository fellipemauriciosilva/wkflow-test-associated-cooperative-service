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
        throw new UnsupportedOperationException("TODO: implement registrarVoto");
    }

    @Transactional(readOnly = true)
    public ResultadoDTO getResultado(Long pautaId) {
        throw new UnsupportedOperationException("TODO: implement getResultado");
    }
}
