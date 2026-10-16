package br.coop.voting.shared.builder;

import br.coop.voting.domain.pauta.dto.CriarPautaRequest;
import br.coop.voting.domain.pauta.entity.Pauta;

public class PautaTestDataBuilder {

    private String titulo = "Pauta Padrão";
    private String descricao = "Descrição padrão válida para testes";

    public PautaTestDataBuilder comTitulo(String titulo) {
        this.titulo = titulo;
        return this;
    }

    public PautaTestDataBuilder comDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }

    public Pauta buildEntity() {
        Pauta pauta = new Pauta();
        pauta.setTitulo(this.titulo);
        pauta.setDescricao(this.descricao);
        return pauta;
    }

    public CriarPautaRequest buildRequest() {
        return new CriarPautaRequest(this.titulo, this.descricao);
    }
}
