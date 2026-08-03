package com.cooperative.associated.domain;

import com.cooperative.associated.domain.exception.InvalidAssociateDataException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link Associate} domain entity.
 * These tests run WITHOUT any Spring context — pure domain logic validation,
 * proving the domain package is framework-agnostic.
 */
class AssociateTest {

    private static final String VALID_NAME = "John Doe";
    private static final String VALID_DOCUMENT = "12345678900";
    private static final String VALID_EMAIL = "john.doe@example.com";

    @Test
    void shouldThrowWhenNameIsNull() {
        assertThrows(InvalidAssociateDataException.class,
                () -> Associate.create(null, VALID_DOCUMENT, VALID_EMAIL));
    }

    @Test
    void shouldThrowWhenNameIsEmpty() {
        assertThrows(InvalidAssociateDataException.class,
                () -> Associate.create("", VALID_DOCUMENT, VALID_EMAIL));
        assertThrows(InvalidAssociateDataException.class,
                () -> Associate.create("   ", VALID_DOCUMENT, VALID_EMAIL));
    }

    @Test
    void shouldThrowWhenDocumentIsMissing() {
        assertThrows(InvalidAssociateDataException.class,
                () -> Associate.create(VALID_NAME, null, VALID_EMAIL));
        assertThrows(InvalidAssociateDataException.class,
                () -> Associate.create(VALID_NAME, "", VALID_EMAIL));
    }

    @Test
    void shouldThrowWhenEmailFormatIsInvalid() {
        assertThrows(InvalidAssociateDataException.class,
                () -> Associate.create(VALID_NAME, VALID_DOCUMENT, "invalid-email"));
        assertThrows(InvalidAssociateDataException.class,
                () -> Associate.create(VALID_NAME, VALID_DOCUMENT, "missing-at-sign.com"));
    }

    @Test
    void shouldCreateSuccessfullyWhenEmailIsValid() {
        Associate associate = Associate.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL);

        assertNotNull(associate.getId());
        assertEquals(VALID_NAME, associate.getName());
        assertEquals(VALID_DOCUMENT, associate.getDocument());
        assertEquals(VALID_EMAIL, associate.getEmail());
        assertNotNull(associate.getCreatedAt());
    }

    @Test
    void shouldCreateSuccessfullyWhenEmailIsAbsent() {
        Associate associate = Associate.create(VALID_NAME, VALID_DOCUMENT, null);

        assertNotNull(associate.getId());
        assertNull(associate.getEmail());
    }

    @Test
    void shouldAllowChangingNameToAValidValue() {
        Associate associate = Associate.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL);

        associate.changeName("Jane Doe");

        assertEquals("Jane Doe", associate.getName());
    }

    @Test
    void shouldThrowWhenChangingNameToBlank() {
        Associate associate = Associate.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL);

        assertThrows(InvalidAssociateDataException.class, () -> associate.changeName(" "));
    }

    @Test
    void shouldThrowWhenChangingEmailToInvalidFormat() {
        Associate associate = Associate.create(VALID_NAME, VALID_DOCUMENT, VALID_EMAIL);

        assertThrows(InvalidAssociateDataException.class, () -> associate.changeEmail("not-an-email"));
    }
}



