package br.coop.voting.domain.sessao.repository;

import br.coop.voting.domain.sessao.entity.Sessao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SessaoRepositoryTest {

    @Autowired
    private SessaoRepository sessaoRepository;

    private Long pautaId;

    @BeforeEach
    void setUp() {
        pautaId = 1L;
    }

    @Test
    void testFindByPautaIdAndAtivaTrueExists() {
        Sessao sessao = new Sessao();
        sessao.setPautaId(pautaId);
        sessao.setDurationMinutes(5);
        sessao.setAtiva(true);

        Sessao saved = sessaoRepository.save(sessao);

        Optional<Sessao> found = sessaoRepository.findByPautaIdAndAtivaTrue(pautaId);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertTrue(found.get().getAtiva());
    }

    @Test
    void testFindByPautaIdAndAtivaTrueNotExists() {
        Sessao sessao = new Sessao();
        sessao.setPautaId(pautaId);
        sessao.setDurationMinutes(5);
        sessao.setAtiva(false);

        sessaoRepository.save(sessao);

        Optional<Sessao> found = sessaoRepository.findByPautaIdAndAtivaTrue(pautaId);

        assertFalse(found.isPresent());
    }

    @Test
    void testFindByAtivaAndDataFechamentoNull() {
        Sessao sessao1 = new Sessao();
        sessao1.setPautaId(1L);
        sessao1.setDurationMinutes(1);
        sessao1.setAtiva(true);
        sessao1.setDataAbertura(LocalDateTime.now().minusMinutes(2));
        sessaoRepository.save(sessao1);

        Sessao sessao2 = new Sessao();
        sessao2.setPautaId(2L);
        sessao2.setDurationMinutes(5);
        sessao2.setAtiva(true);
        sessao2.setDataAbertura(LocalDateTime.now());
        sessaoRepository.save(sessao2);

        Sessao sessao3 = new Sessao();
        sessao3.setPautaId(3L);
        sessao3.setDurationMinutes(10);
        sessao3.setAtiva(false);
        sessao3.setDataFechamento(LocalDateTime.now());
        sessaoRepository.save(sessao3);

        List<Sessao> ativas = sessaoRepository.findByAtivaAndDataFechamentoNull();

        assertEquals(2, ativas.size());
        assertTrue(ativas.stream().anyMatch(s -> s.getPautaId().equals(1L)));
        assertTrue(ativas.stream().anyMatch(s -> s.getPautaId().equals(2L)));
    }

    @Test
    void testFindByAtivaAndDataFechamentoNullEmpty() {
        Sessao sessao = new Sessao();
        sessao.setPautaId(1L);
        sessao.setDurationMinutes(60);
        sessao.setAtiva(false);
        sessao.setDataFechamento(LocalDateTime.now());
        sessaoRepository.save(sessao);

        List<Sessao> ativas = sessaoRepository.findByAtivaAndDataFechamentoNull();

        assertTrue(ativas.isEmpty());
    }

    @Test
    void testUniquePautaIdConstraint() {
        Sessao sessao1 = new Sessao();
        sessao1.setPautaId(pautaId);
        sessao1.setDurationMinutes(5);
        sessao1.setAtiva(true);
        sessaoRepository.save(sessao1);

        Sessao sessao2 = new Sessao();
        sessao2.setPautaId(pautaId);
        sessao2.setDurationMinutes(10);
        sessao2.setAtiva(true);

        assertThrows(Exception.class, () -> {
            sessaoRepository.save(sessao2);
            sessaoRepository.flush();
        });
    }
}
