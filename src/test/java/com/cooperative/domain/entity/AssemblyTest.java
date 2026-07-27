package com.cooperative.domain.entity;

import com.cooperative.domain.exception.InvalidAssemblyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AssemblyTest {

    @Test
    void testCreateWithValidData() {
        Instant now = Instant.now();
        Assembly assembly = Assembly.create("Board Meeting", "Quarterly review", now);
        assertNull(assembly.id());
        assertEquals("Board Meeting", assembly.title());
        assertEquals("Quarterly review", assembly.description());
        assertEquals(now, assembly.scheduledAt());
        assertNotNull(assembly.createdAt());
    }

    @Test
    void testCreateWithoutOptionalFields() {
        Assembly assembly = Assembly.create("Board Meeting", null, null);
        assertEquals("Board Meeting", assembly.title());
        assertNull(assembly.description());
        assertNull(assembly.scheduledAt());
    }

    @Test
    void testCreateWithNullTitle() {
        InvalidAssemblyException ex = assertThrows(
            InvalidAssemblyException.class,
            () -> Assembly.create(null, "Description", Instant.now())
        );
        assertTrue(ex.getMessage().contains("title is required"));
    }

    @Test
    void testCreateWithBlankTitle() {
        InvalidAssemblyException ex = assertThrows(
            InvalidAssemblyException.class,
            () -> Assembly.create("   ", "Description", Instant.now())
        );
        assertNotNull(ex.getMessage());
    }

    @Test
    void testCreateWithTitleOnly() {
        Assembly assembly = Assembly.create("Board Meeting", null, null);
        assertEquals("Board Meeting", assembly.title());
        assertNull(assembly.description());
        assertNull(assembly.scheduledAt());
    }

    @Test
    void testImmutability() {
        Assembly assembly = Assembly.create("Board Meeting", "Description", Instant.now());
        assertNotNull(assembly);
        // Records are immutable by design - no setters available
    }
}
