package br.coop.assembly.dto;

import br.coop.assembly.entity.Voto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotoDTO {
    private Long id;
    private Long pautaId;
    private String associadoId;
    private String voto;
    private String timestamp;

    public static VotoDTO fromEntity(Voto voto) {
        VotoDTO dto = new VotoDTO();
        dto.setId(voto.getId());
        if (voto.getPauta() != null) {
            dto.setPautaId(voto.getPauta().getId());
        }
        dto.setAssociadoId(voto.getAssociadoId());
        dto.setVoto(voto.getVoto().name());
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        dto.setTimestamp(voto.getTimestamp().format(formatter));
        return dto;
    }
}
