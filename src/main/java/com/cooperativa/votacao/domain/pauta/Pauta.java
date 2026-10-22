package com.cooperativa.votacao.domain.pauta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Pauta {

    public static final int TITULO_TAMANHO_MAXIMO = 200;
    public static final int DESCRICAO_TAMANHO_MAXIMO = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TITULO_TAMANHO_MAXIMO)
    private String titulo;

    @Column(length = DESCRICAO_TAMANHO_MAXIMO)
    private String descricao;

    protected Pauta() {
        // exigido pelo JPA
    }

    public Pauta(String titulo, String descricao) {
        validarTitulo(titulo);
        validarDescricao(descricao);
        this.titulo = titulo.strip();
        this.descricao = descricao == null ? null : descricao.strip();
    }

    private static void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank() || !temCaractereVisivel(titulo)) {
            throw new PautaInvalidaException("titulo e obrigatorio");
        }
        var tituloStripado = titulo.strip();
        if (tituloStripado.codePointCount(0, tituloStripado.length()) > TITULO_TAMANHO_MAXIMO) {
            throw new PautaInvalidaException(
                    "titulo deve ter no maximo " + TITULO_TAMANHO_MAXIMO + " caracteres");
        }
    }

    // isBlank()/strip() usam Character.isWhitespace, que nao cobre NBSP nem categorias
    // "invisiveis" como separadores Unicode (Zs/Zl/Zp) ou zero-width (Cf). Sem este check,
    // um titulo so com esses caracteres passava como "nao vazio" e virava "titulo fantasma"
    // (nada visivel na UI, mas tecnicamente preenchido) — achado do review de seguranca.
    private static boolean temCaractereVisivel(String texto) {
        return texto.codePoints().anyMatch(codePoint -> switch (Character.getType(codePoint)) {
            case Character.SPACE_SEPARATOR, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR,
                    Character.CONTROL, Character.FORMAT, Character.SURROGATE, Character.UNASSIGNED -> false;
            default -> true;
        });
    }

    private static void validarDescricao(String descricao) {
        if (descricao == null) {
            return;
        }
        var descricaoStripada = descricao.strip();
        if (descricaoStripada.codePointCount(0, descricaoStripada.length()) > DESCRICAO_TAMANHO_MAXIMO) {
            throw new PautaInvalidaException(
                    "descricao deve ter no maximo " + DESCRICAO_TAMANHO_MAXIMO + " caracteres");
        }
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
