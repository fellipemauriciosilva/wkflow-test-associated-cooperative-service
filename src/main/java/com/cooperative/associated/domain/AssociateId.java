package com.cooperative.associated.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object wrapping the identity of an {@link Associate}.
 * Pure domain type - no framework dependencies.
 */
public final class AssociateId {

    private final UUID value;

    private AssociateId(UUID value) {
        this.value = Objects.requireNonNull(value, "id must not be null");
    }

    public static AssociateId of(UUID value) {
        return new AssociateId(value);
    }

    public static AssociateId generate() {
        return new AssociateId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssociateId that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
