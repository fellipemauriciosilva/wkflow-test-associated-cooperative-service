package br.coop.assembly.dto;

import br.coop.assembly.entity.Pauta;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PautaResponse {
    private Long id;
    private String title;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static PautaResponse from(Pauta pauta) {
        return new PautaResponse(
                pauta.getId(),
                pauta.getTitle(),
                pauta.getDescription(),
                pauta.getCreatedAt(),
                pauta.getUpdatedAt()
        );
    }
}
