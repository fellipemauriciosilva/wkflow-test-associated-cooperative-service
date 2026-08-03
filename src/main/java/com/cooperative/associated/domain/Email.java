package com.cooperative.associated.domain;

import com.cooperative.associated.domain.exception.InvalidAssociateDataException;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Value object representing an Associate's optional email address.
 * When present, must match a valid email format. Pure domain type -
 * no framework dependencies.
 */
public final class Email {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new Email(null);
        }
        if (!EMAIL_PATTERN.matcher(rawValue).matches()) {
            throw new InvalidAssociateDataException("email format is invalid");
        }
        return new Email(rawValue.trim());
    }

    public static Email empty() {
        return new Email(null);
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    public boolean isPresent() {
        return value != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value == null ? "" : value;
    }
}
