package br.coop.assembleia.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarPautaRequest(
        @NotBlank
        @Pattern(regexp = "(?s).*[^\\p{Z}\\p{C}\\s].*")
        @Size(max = 255)
        String titulo,

        @Size(max = 255)
        String descricao) {
}
