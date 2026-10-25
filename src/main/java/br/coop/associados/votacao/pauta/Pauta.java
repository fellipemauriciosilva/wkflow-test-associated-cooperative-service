package br.coop.associados.votacao.pauta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Pauta {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = PautaRequest.TITULO_MAX)
    private String titulo;

    @Column(length = PautaRequest.DESCRICAO_MAX)
    private String descricao;

    protected Pauta() {
    }

    public Pauta(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }
}
