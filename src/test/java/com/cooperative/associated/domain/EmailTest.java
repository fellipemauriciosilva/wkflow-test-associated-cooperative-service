package com.cooperative.associated.domain;

import com.cooperative.associated.domain.exception.InvalidAssociateDataException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTest {

    @Test
    void shouldCreateEmptyEmailWhenRawValueIsNull() {
        Email email = Email.of(null);

        assertFalse(email.isPresent());
        assertEquals(Optional.empty(), email.getValue());
    }

    @Test
    void shouldCreateEmptyEmailWhenRawValueIsBlank() {
        Email email = Email.of("   ");

        assertFalse(email.isPresent());
        assertEquals(Optional.empty(), email.getValue());
    }

    @Test
    void shouldCreateValidEmailWhenFormatIsCorrect() {
        Email email = Email.of("john.doe@example.com");

        assertTrue(email.isPresent());
        assertEquals(Optional.of("john.doe@example.com"), email.getValue());
    }

    @Test
    void shouldThrowWhenFormatIsInvalid() {
        assertThrows(InvalidAssociateDataException.class, () -> Email.of("invalid-email"));
    }

    @Test
    void shouldThrowWhenMissingDomainSuffix() {
        assertThrows(InvalidAssociateDataException.class, () -> Email.of("a@b"));
    }

    @Test
    void emptyFactoryShouldReturnAbsentEmail() {
        Email email = Email.empty();

        assertFalse(email.isPresent());
        assertEquals(Optional.empty(), email.getValue());
    }

    @Test
    void equalEmailsShouldBeEqualAndHaveSameHashCode() {
        Email a = Email.of("a@b.com");
        Email b = Email.of("a@b.com");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}


