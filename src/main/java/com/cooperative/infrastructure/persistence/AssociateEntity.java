package com.cooperative.infrastructure.persistence;

import com.cooperative.domain.entity.Associate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for Associate persistence.
 * Infrastructure detail, not exposed to application/domain layers.
 */
@Entity
@Table(
    name = "associates",
    indexes = {
        @Index(name = "idx_document", columnList = "document"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
public class AssociateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String document;

    @Column(nullable = true)
    private String email;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors

    public AssociateEntity() {
    }

    public AssociateEntity(String name, String document, String email, Instant createdAt) {
        this.name = name;
        this.document = document;
        this.email = email;
        this.createdAt = createdAt;
    }

    // Domain conversion

    /**
     * Converts this JPA entity to domain entity.
     *
     * @return domain Associate entity
     */
    public Associate toDomain() {
        return new Associate(id, name, document, email, createdAt);
    }

    /**
     * Converts domain entity to JPA entity.
     *
     * @param associate the domain entity
     * @return JPA entity (without ID, assigned by database)
     */
    public static AssociateEntity fromDomain(Associate associate) {
        return new AssociateEntity(
            associate.name(),
            associate.document(),
            associate.email(),
            associate.createdAt()
        );
    }

    // Getters

    public Long getId() {
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
