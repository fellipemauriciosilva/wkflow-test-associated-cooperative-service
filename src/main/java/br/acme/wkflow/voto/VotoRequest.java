package br.acme.wkflow.voto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VotoRequest(@NotBlank @Size(max = 255) String associadoId, String valor) {

    public VotoRequest {
        if (associadoId != null) {
            associadoId = associadoId.strip();
        }
    }
}
