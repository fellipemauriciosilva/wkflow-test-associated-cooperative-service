package br.acme.pauta;

public record ErroPadrao(int status, String error, String message, String path) {
}
