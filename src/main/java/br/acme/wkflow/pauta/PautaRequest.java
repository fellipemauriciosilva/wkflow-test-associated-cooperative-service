package br.acme.wkflow.pauta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(@NotBlank @Size(max = 255) String descricao) {
}
