package br.coop.assembly.dto;

import br.coop.assembly.entity.Sessao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoResponse {
    private Long sessionId;
    private Long pautaId;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private String status;
    private String mensagem;

    public static SessaoResponse from(Sessao sessao) {
        return SessaoResponse.builder()
                .sessionId(sessao.getId())
                .pautaId(sessao.getPautaId())
                .dataInicio(sessao.getDataInicio())
                .dataFim(sessao.getDataFim())
                .status(sessao.getStatus().toString())
                .mensagem("Sessao aberta com sucesso")
                .build();
    }
}
