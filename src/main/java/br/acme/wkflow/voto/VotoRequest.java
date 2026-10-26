package br.acme.wkflow.voto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VotoRequest(
        @NotBlank
        @Size(max = 255)
        @Pattern(regexp = "^(?:[^\\p{Cc}\\p{Cf}\\p{Zs}\\p{Zl}\\p{Zp}]|[ ])*$")
        String associadoId,
        String valor) {

    public VotoRequest {
        if (associadoId != null) {
            associadoId = associadoId.replaceAll("^ +| +$", "");
        }
    }
}
