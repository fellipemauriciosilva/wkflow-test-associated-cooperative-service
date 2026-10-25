package br.coop.pautas.pauta;

public record PautaResponse(Long id, String titulo, String descricao) {

    static PautaResponse from(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao());
    }
}
