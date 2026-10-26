package br.acme.wkflow.sessao;

import jakarta.validation.constraints.Min;

public record SessaoRequest(@Min(1) Integer duracaoMinutos) {
}
