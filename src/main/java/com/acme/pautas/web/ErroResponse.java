package com.acme.pautas.web;

public record ErroResponse(int status, String erro, String mensagem, String path) {
}
