package com.cooperative.associated.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "associates")
public class AssociateJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String document;

    @Column
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AssociateJpaEntity() {
        // JPA
    }

    public AssociateJpaEntity(UUID id, String name, String document, String email, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.document = document;
        this.email = email;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDocument() {
        return document;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

