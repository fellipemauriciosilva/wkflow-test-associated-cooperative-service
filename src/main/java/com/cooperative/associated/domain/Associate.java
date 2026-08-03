package com.cooperative.associated.domain;

import com.cooperative.associated.domain.exception.InvalidAssociateDataException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Associate domain entity. Pure Java - MUST NOT depend on Spring, JPA,
 * Kafka, or any HTTP/framework class. All business invariants are enforced
 * here, not merely via framework annotations.
 */
public final class Associate {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UUID id;
    private String name;
    private String document;
    private String email;
    private final Instant createdAt;

    private Associate(UUID id, String name, String document, String email, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        changeName(name);
        changeDocument(document);
        changeEmail(email);
    }

    public static Associate create(String name, String document, String email) {
        return new Associate(UUID.randomUUID(), name, document, email, Instant.now());
    }

    public static Associate restore(UUID id, String name, String document, String email, Instant createdAt) {
        return new Associate(id, name, document, email, createdAt);
    }

    public void changeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidAssociateDataException("name is required");
        }
        this.name = name.trim();
    }

    public void changeDocument(String document) {
        if (document == null || document.isBlank()) {
            throw new InvalidAssociateDataException("document is required");
        }
        this.document = document.trim();
    }

    public void changeEmail(String email) {
        if (email == null || email.isBlank()) {
            this.email = null;
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidAssociateDataException("email has invalid format");
        }
        this.email = email.trim();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Associate that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

