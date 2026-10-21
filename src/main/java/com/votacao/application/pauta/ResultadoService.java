package com.votacao.application.pauta;

import com.votacao.api.pauta.dto.ResultadoResponse;
import com.votacao.domain.sessao.Sessao;
import com.votacao.domain.sessao.SessaoRepository;
import com.votacao.domain.sessao.StatusSessao;
import com.votacao.domain.voto.TipoVoto;
import com.votacao.domain.voto.VotoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

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

    public ResultadoResponse apurar(Long pautaId) {
        pautaService.buscarPorId(pautaId);
        Optional<Sessao> sessao = sessaoRepository.findByPautaId(pautaId);

        StatusSessao statusSessao = derivarStatus(sessao);

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

        return new ResultadoResponse(
                votosSim, votosNao, statusSessao, votosSim + votosNao, LocalDateTime.now(clock));
    }

    private StatusSessao derivarStatus(Optional<Sessao> sessao) {
        if (sessao.isEmpty()) {
            return StatusSessao.NAO_INICIADA;
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        return agora.isBefore(sessao.get().getFechamento()) ? StatusSessao.ABERTA : StatusSessao.EXPIRADA;
    }
}
