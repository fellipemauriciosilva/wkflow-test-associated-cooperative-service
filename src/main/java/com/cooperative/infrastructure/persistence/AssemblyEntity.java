package com.cooperative.infrastructure.persistence;

import com.cooperative.domain.entity.Assembly;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for Assembly persistence.
 * Infrastructure detail, not exposed to application/domain layers.
 */
@Entity
@Table(
    name = "assemblies",
    indexes = {
        @Index(name = "idx_title", columnList = "title"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_scheduled_at", columnList = "scheduled_at")
    }
)
public class AssemblyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = true)
    private Instant scheduledAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors

    public AssemblyEntity() {
    }

    public AssemblyEntity(String title, String description, Instant scheduledAt, Instant createdAt) {
        this.title = title;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.createdAt = createdAt;
    }

    // Domain conversion

    /**
     * Converts this JPA entity to domain entity.
     *
     * @return domain Assembly entity
     */
    public Assembly toDomain() {
        return new Assembly(id, title, description, scheduledAt, createdAt);
    }

    /**
     * Converts domain entity to JPA entity.
     *
     * @param assembly the domain entity
     * @return JPA entity (without ID, assigned by database)
     */
    public static AssemblyEntity fromDomain(Assembly assembly) {
        return new AssemblyEntity(
            assembly.title(),
            assembly.description(),
            assembly.scheduledAt(),
            assembly.createdAt()
        );
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
