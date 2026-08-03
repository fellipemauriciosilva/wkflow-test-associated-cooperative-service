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

    private static final Email EMPTY = new Email(null);

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return EMPTY;
        }
        String trimmed = rawValue.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidAssociateDataException("email format is invalid");
        }
        return new Email(trimmed);
    }

    public static Email empty() {
        return EMPTY;
    }

    public boolean isPresent() {
        return value != null;
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Email email)) {
            return false;
        }
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value == null ? "" : value;
    }
}


