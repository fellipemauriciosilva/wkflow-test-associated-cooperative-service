package com.cooperative.application.dto;

import com.cooperative.domain.entity.Assembly;
import java.time.Instant;

/**
 * Response DTO for Assembly entity.
 */
public record AssemblyResponse(
    Long id,
    String title,
    String description,
    Instant scheduledAt,
    Instant createdAt
) {

    /**
     * Converts domain Assembly entity to response DTO.
     *
     * @param assembly the domain entity
     * @return response DTO
     */
    public static AssemblyResponse fromDomain(Assembly assembly) {
        return new AssemblyResponse(
            assembly.id(),
            assembly.title(),
            assembly.description(),
            assembly.scheduledAt(),
            assembly.createdAt()
        );
    }
}
