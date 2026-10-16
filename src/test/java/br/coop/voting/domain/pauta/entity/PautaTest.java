package br.coop.voting.domain.pauta.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PautaTest {

    @Test
    void testConstructorSemArgumentos() {
        Pauta pauta = new Pauta();
        assertNull(pauta.getId());
        assertNull(pauta.getTitulo());
        assertNull(pauta.getDescricao());
    }

    @Test
    void testConstructorComArgumentos() {
        Pauta pauta = new Pauta(1L, "Título", "Descrição teste", null, null);
        assertEquals(1L, pauta.getId());
        assertEquals("Título", pauta.getTitulo());
        assertEquals("Descrição teste", pauta.getDescricao());
    }

    @Test
    void testGettersSetters() {
        Pauta pauta = new Pauta();
        pauta.setId(1L);
        pauta.setTitulo("Novo Título");
        pauta.setDescricao("Nova Descrição");

        assertEquals(1L, pauta.getId());
        assertEquals("Novo Título", pauta.getTitulo());
        assertEquals("Nova Descrição", pauta.getDescricao());
    }

    @Test
    void testEqualsHashCode() {
        Pauta pauta1 = new Pauta(1L, "Título", "Descrição", null, null);
        Pauta pauta2 = new Pauta(1L, "Título diferente", "Descrição diferente", null, null);
        Pauta pauta3 = new Pauta(2L, "Título", "Descrição", null, null);

        assertEquals(pauta1, pauta2); // Iguais porque id é igual
        assertNotEquals(pauta1, pauta3); // Diferentes porque id é diferente
        assertEquals(pauta1.hashCode(), pauta2.hashCode());
        assertNotEquals(pauta1.hashCode(), pauta3.hashCode());
    }

    @Test
    void testToString() {
        Pauta pauta = new Pauta(1L, "Título", "Descrição", null, null);
        String toString = pauta.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Pauta"));
        assertTrue(toString.contains("Título"));
    }
}
