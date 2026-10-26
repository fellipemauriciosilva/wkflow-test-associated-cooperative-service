package br.acme.wkflow.sessao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SessaoRequest(@Min(1) @Max(1440) Integer duracaoMinutos) {
}
