package com.cooperativa.votacao.domain.sessao;

import com.cooperativa.votacao.domain.pauta.Pauta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
public class Sessao {

    public static final int DURACAO_PADRAO_MINUTOS = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(nullable = false)
    private Integer duracaoMinutos;

    @Column(nullable = false)
    private Instant abertura;

    protected Sessao() {
        // exigido pelo JPA
    }

    public Sessao(Pauta pauta, Integer duracaoMinutos, Instant abertura) {
        validarDuracao(duracaoMinutos);
        this.pauta = Objects.requireNonNull(pauta, "pauta e obrigatoria");
        this.duracaoMinutos = duracaoMinutos;
        this.abertura = Objects.requireNonNull(abertura, "abertura e obrigatoria");
    }

    // Chamado tambem pelo service, antes de buscar a pauta, pois a ordem de validacao exigida
    // e corpo (400) -> pauta existe (404) -> sessao aberta (409). Se a validacao ficasse so no
    // construtor, duracao invalida com pauta inexistente responderia 404 em vez de 400.
    public static void validarDuracao(Integer duracaoMinutos) {
        if (duracaoMinutos == null || duracaoMinutos <= 0) {
            throw new SessaoInvalidaException("duracao deve ser maior que zero");
        }
    }

    public Instant getFim() {
        return abertura.plus(duracaoMinutos, ChronoUnit.MINUTES);
    }

    public boolean estaAberta(Instant agora) {
        return getFim().isAfter(agora);
    }

    public Long getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public Integer getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public Instant getAbertura() {
        return abertura;
    }
}
