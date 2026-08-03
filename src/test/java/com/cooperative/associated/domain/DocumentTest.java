package com.cooperative.associated.domain;

import com.cooperative.associated.domain.exception.InvalidAssociateDataException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentTest {

    @Test
    void shouldThrowWhenValueIsNull() {
        assertThrows(InvalidAssociateDataException.class, () -> Document.of(null));
    }

    @Test
    void shouldThrowWhenValueIsBlank() {
        assertThrows(InvalidAssociateDataException.class, () -> Document.of("   "));
    }

    @Test
    void shouldThrowWhenValueIsEmpty() {
        assertThrows(InvalidAssociateDataException.class, () -> Document.of(""));
    }

    @Test
    void shouldCreateSuccessfullyWithValidValue() {
        Document document = Document.of("12345678900");
        assertEquals("12345678900", document.getValue());
    }

    @Test
    void shouldTrimValue() {
        Document document = Document.of("  12345678900  ");
        assertEquals("12345678900", document.getValue());
    }

    @Test
    void equalDocumentsShouldBeEqual() {
        Document a = Document.of("12345678900");
        Document b = Document.of("12345678900");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}


