package br.coop.voting.domain.pauta.repository;

import br.coop.voting.domain.pauta.entity.Pauta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PautaRepositoryTest {

    @Autowired
    private PautaRepository pautaRepository;

    private Pauta pauta1;
    private Pauta pauta2;

    @BeforeEach
    void setUp() {
        pauta1 = new Pauta();
        pauta1.setTitulo("Pauta Primeira");
        pauta1.setDescricao("Descrição da primeira pauta");

        pauta2 = new Pauta();
        pauta2.setTitulo("Pauta Segunda");
        pauta2.setDescricao("Descrição da segunda pauta");

        pautaRepository.save(pauta1);
        pautaRepository.save(pauta2);
    }

    @Test
    void testSaveAndRetrieveById() {
        Pauta pauta = new Pauta();
        pauta.setTitulo("Pauta Teste");
        pauta.setDescricao("Descrição teste para salvar");

        Pauta salva = pautaRepository.save(pauta);

        Optional<Pauta> recuperada = pautaRepository.findById(salva.getId());

        assertTrue(recuperada.isPresent());
        assertEquals("Pauta Teste", recuperada.get().getTitulo());
        assertEquals("Descrição teste para salvar", recuperada.get().getDescricao());
    }

    @Test
    void testFindByTituloContainingIgnoreCase() {
        List<Pauta> resultado = pautaRepository.findByTituloContainingIgnoreCase("primeira");

        assertEquals(1, resultado.size());
        assertEquals("Pauta Primeira", resultado.get(0).getTitulo());
    }

    @Test
    void testFindByTituloContainingIgnoreCaseComMultiplosResultados() {
        List<Pauta> resultado = pautaRepository.findByTituloContainingIgnoreCase("Pauta");

        assertEquals(2, resultado.size());
    }

    @Test
    void testFindByTituloExact() {
        Optional<Pauta> resultado = pautaRepository.findByTituloExact("Pauta Primeira");

        assertTrue(resultado.isPresent());
        assertEquals("Pauta Primeira", resultado.get().getTitulo());
    }

    @Test
    void testFindByTituloExactCaseInsensitive() {
        Optional<Pauta> resultado = pautaRepository.findByTituloExact("pauta primeira");

        assertTrue(resultado.isPresent());
        assertEquals("Pauta Primeira", resultado.get().getTitulo());
    }

    @Test
    void testFindByTituloExactNaoEncontrado() {
        Optional<Pauta> resultado = pautaRepository.findByTituloExact("Pauta Inexistente");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void testFindAll() {
        List<Pauta> pautas = pautaRepository.findAll();

        assertEquals(2, pautas.size());
    }

    @Test
    void testDelete() {
        pautaRepository.delete(pauta1);
        List<Pauta> pautas = pautaRepository.findAll();

        assertEquals(1, pautas.size());
        assertFalse(pautas.contains(pauta1));
    }

    @Test
    void testUpdate() {
        pauta1.setTitulo("Pauta Primeira Atualizada");
        Pauta atualizada = pautaRepository.save(pauta1);

        Optional<Pauta> recuperada = pautaRepository.findById(atualizada.getId());

        assertTrue(recuperada.isPresent());
        assertEquals("Pauta Primeira Atualizada", recuperada.get().getTitulo());
    }
}
