package com.cooperative.associated.domain;

import com.cooperative.associated.domain.exception.InvalidAssociateDataException;

import java.util.Objects;

/**
 * Value object representing an Associate's unique document (e.g. CPF/CNPJ).
 * Pure domain type - no framework dependencies.
 */
public final class Document {

    private final String value;

    private Document(String value) {
        this.value = value;
    }

    public static Document of(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new InvalidAssociateDataException("document must not be null or blank");
        }
        return new Document(rawValue.trim());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document document)) {
            return false;
        }
        return value.equals(document.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
