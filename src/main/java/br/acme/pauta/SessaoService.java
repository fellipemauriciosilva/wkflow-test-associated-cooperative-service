package br.acme.pauta;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessaoService {

    private final PautaRepository pautas;
    private final SessaoRepository sessoes;
    private final Clock relogio;

    public SessaoService(PautaRepository pautas, SessaoRepository sessoes, Clock relogio) {
        this.pautas = pautas;
        this.sessoes = sessoes;
        this.relogio = relogio;
    }

    @Transactional
    public SessaoResponse abrir(Long pautaId, SessaoRequest request) {
        Pauta pauta = pautas.findComLockPorId(pautaId)
                .orElseThrow(() -> new PautaNaoEncontradaException(pautaId));
        Instant agora = relogio.instant();
        if (sessoes.countAbertasEm(pautaId, agora) > 0) {
            throw new SessaoAbertaException(pautaId);
        }
        int duracao = request == null ? SessaoRequest.DURACAO_PADRAO_MINUTOS : request.duracaoEmMinutos();
        Instant fim = agora.plus(Duration.ofMinutes(duracao));
        return SessaoResponse.de(sessoes.save(new Sessao(pauta, agora, fim)));
    }
}
