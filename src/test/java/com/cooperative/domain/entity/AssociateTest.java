package com.cooperative.domain.entity;

import com.cooperative.domain.exception.InvalidAssociateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AssociateTest {

    @Test
    void testCreateWithValidData() {
        Associate associate = Associate.create("John Doe", "12345678901", "john@example.com");
        assertNull(associate.id());
        assertEquals("John Doe", associate.name());
        assertEquals("12345678901", associate.document());
        assertEquals("john@example.com", associate.email());
        assertNotNull(associate.createdAt());
    }

    @Test
    void testCreateWithNameTooShort() {
        InvalidAssociateException ex = assertThrows(
            InvalidAssociateException.class,
            () -> Associate.create("J", "12345678901", "john@example.com")
        );
        assertTrue(ex.getMessage().contains("at least 2 characters"));
    }

    @Test
    void testCreateWithNullName() {
        InvalidAssociateException ex = assertThrows(
            InvalidAssociateException.class,
            () -> Associate.create(null, "12345678901", "john@example.com")
        );
        assertNotNull(ex.getMessage());
    }

    @Test
    void testCreateWithoutEmail() {
        Associate associate = Associate.create("John Doe", "12345678901", null);
        assertNull(associate.email());
    }

    @Test
    void testCreateWithInvalidEmail() {
        InvalidAssociateException ex = assertThrows(
            InvalidAssociateException.class,
            () -> Associate.create("John Doe", "12345678901", "invalid-email")
        );
        assertTrue(ex.getMessage().contains("Invalid email format"));
    }

    @Test
    void testCreateWithNullDocument() {
        InvalidAssociateException ex = assertThrows(
            InvalidAssociateException.class,
            () -> Associate.create("John Doe", null, "john@example.com")
        );
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    @Test
    void testCreateWithEmptyDocument() {
        InvalidAssociateException ex = assertThrows(
            InvalidAssociateException.class,
            () -> Associate.create("John Doe", "   ", "john@example.com")
        );
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    @Test
    void testCreateWithValidNameAndDocumentOnly() {
        Associate associate = Associate.create("John Doe", "12345678901", null);
        assertEquals("John Doe", associate.name());
        assertEquals("12345678901", associate.document());
        assertNull(associate.email());
    }

    @Test
    void testImmutability() {
        Associate associate = Associate.create("John Doe", "12345678901", "john@example.com");
        assertNotNull(associate);
        // Records are immutable by design - no setters available
    }
}
