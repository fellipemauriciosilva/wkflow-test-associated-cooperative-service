package com.votacao.application.pauta;

import com.votacao.api.pauta.dto.ResultadoResponse;
import com.votacao.domain.sessao.Sessao;
import com.votacao.domain.sessao.SessaoRepository;
import com.votacao.domain.sessao.StatusSessao;
import com.votacao.domain.voto.TipoVoto;
import com.votacao.domain.voto.VotoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultadoService {

    private final PautaService pautaService;
    private final SessaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final Clock clock;

    public ResultadoService(
            PautaService pautaService,
            SessaoRepository sessaoRepository,
            VotoRepository votoRepository,
            Clock clock) {
        this.pautaService = pautaService;
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResultadoResponse apurar(Long pautaId) {
        pautaService.buscarPorId(pautaId);
        Optional<Sessao> sessao = sessaoRepository.findByPautaId(pautaId);

        Instant instante = clock.instant();
        LocalDateTime agora = LocalDateTime.ofInstant(instante, clock.getZone());
        StatusSessao statusSessao = derivarStatus(sessao, agora);

        long votosSim = 0L;
        long votosNao = 0L;
        if (sessao.isPresent()) {
            for (VotoRepository.ContagemPorTipo contagem : votoRepository.contarPorTipo(pautaId)) {
                if (contagem.getTipo() == TipoVoto.SIM) {
                    votosSim = contagem.getTotal();
                } else if (contagem.getTipo() == TipoVoto.NAO) {
                    votosNao = contagem.getTotal();
                }
            }
        }

        OffsetDateTime timestamp = OffsetDateTime.ofInstant(instante, clock.getZone());
        return new ResultadoResponse(votosSim, votosNao, statusSessao, votosSim + votosNao, timestamp);
    }

    private StatusSessao derivarStatus(Optional<Sessao> sessao, LocalDateTime agora) {
        if (sessao.isEmpty()) {
            return StatusSessao.NAO_INICIADA;
        }
        return agora.isBefore(sessao.get().getFechamento()) ? StatusSessao.ABERTA : StatusSessao.EXPIRADA;
    }
}
