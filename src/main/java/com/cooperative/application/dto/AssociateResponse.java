package com.cooperative.application.dto;

import com.cooperative.domain.entity.Associate;
import java.time.Instant;

/**
 * Response DTO for Associate entity.
 */
public record AssociateResponse(
    Long id,
    String name,
    String document,
    String email,
    Instant createdAt
) {

    /**
     * Converts domain Associate entity to response DTO.
     *
     * @param associate the domain entity
     * @return response DTO
     */
    public static AssociateResponse fromDomain(Associate associate) {
        return new AssociateResponse(
            associate.id(),
            associate.name(),
            associate.document(),
            associate.email(),
            associate.createdAt()
        );
    }
}
