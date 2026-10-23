package pautas;

import java.time.Instant;

public record SessaoResponse(Long id, Long pautaId, Instant abertura, Instant fim) {

    public static SessaoResponse de(Sessao sessao) {
        return new SessaoResponse(sessao.getId(), sessao.getPauta().getId(), sessao.getAbertura(), sessao.getFim());
    }
}
