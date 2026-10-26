package br.acme.wkflow.pauta;

public record PautaResponse(Long id, String descricao) {

    static PautaResponse de(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getDescricao());
    }
}
