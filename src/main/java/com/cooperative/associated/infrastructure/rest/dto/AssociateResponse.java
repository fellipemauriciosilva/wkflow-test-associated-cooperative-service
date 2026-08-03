package com.cooperative.associated.infrastructure.rest.dto;

import com.cooperative.associated.domain.Associate;

import java.time.Instant;
import java.util.UUID;

public record AssociateResponse(
        UUID id,
        String name,
        String document,
        String email,
        Instant createdAt
) {
    public static AssociateResponse from(Associate associate) {
        return new AssociateResponse(
                associate.getId(),
                associate.getName(),
                associate.getDocument(),
                associate.getEmail(),
                associate.getCreatedAt()
        );
    }
}

