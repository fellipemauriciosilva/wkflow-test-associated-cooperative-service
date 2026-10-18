package com.example.pauta.dto;

import com.example.pauta.entity.Pauta;

import java.time.Instant;

public record PautaResponse(Long id, String title, Instant createdAt) {

    public static PautaResponse fromEntity(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getTitle(), pauta.getCreatedAt());
    }
}
