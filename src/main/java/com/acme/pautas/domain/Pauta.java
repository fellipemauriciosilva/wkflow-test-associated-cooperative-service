package com.acme.pautas.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Pauta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    private String descricao;

    protected Pauta() {
    }

    private Pauta(String titulo, String descricao) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("titulo obrigatorio");
        }
        this.titulo = titulo;
        this.descricao = descricao;
    }

    public static Pauta criar(String titulo, String descricao) {
        return new Pauta(titulo, descricao);
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
