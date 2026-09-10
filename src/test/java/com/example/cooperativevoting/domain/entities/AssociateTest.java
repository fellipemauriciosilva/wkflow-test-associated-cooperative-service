package com.example.cooperativevoting.domain.entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssociateTest {

    @Test
    void testAssociateCreation() {
        UUID id = UUID.randomUUID();
        String name = "John Doe";
        String document = "12345678901";
        String email = "john@example.com";

        Associate associate = new Associate(id, name, document, email);

        assertEquals(id, associate.getId());
        assertEquals(name, associate.getName());
        assertEquals(document, associate.getDocument());
        assertEquals(email, associate.getEmail());
        assertNotNull(associate.getCreatedAt());
        assertNotNull(associate.getUpdatedAt());
    }

    @Test
    void testAssociateEquality() {
        UUID id = UUID.randomUUID();
        Associate associate1 = new Associate(id, "John", "doc1", "john@example.com");
        Associate associate2 = new Associate(id, "Jane", "doc2", "jane@example.com");

        assertEquals(associate1, associate2);
        assertEquals(associate1.hashCode(), associate2.hashCode());
    }

    @Test
    void testAssociateInequality() {
        Associate associate1 = new Associate(UUID.randomUUID(), "John", "doc1", "john@example.com");
        Associate associate2 = new Associate(UUID.randomUUID(), "John", "doc2", "jane@example.com");

        assertNotEquals(associate1, associate2);
    }

    @Test
    void testAssociateFieldUpdate() {
        Associate associate = new Associate(UUID.randomUUID(), "John", "doc1", "john@example.com");

        associate.setName("Jane");
        associate.setEmail("jane@example.com");

        assertEquals("Jane", associate.getName());
        assertEquals("jane@example.com", associate.getEmail());
    }
}
