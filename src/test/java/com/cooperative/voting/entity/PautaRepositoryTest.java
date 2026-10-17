package com.cooperative.voting.entity;

import com.cooperative.voting.repository.PautaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises JPA lifecycle callbacks ({@code @PrePersist}/{@code @PreUpdate}) on {@link Pauta}.
 */
@DataJpaTest
@DirtiesContext
class PautaRepositoryTest {

    @Autowired
    private PautaRepository pautaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testTimestampsAreSetOnCreateAndUpdate() {
        Pauta pauta = Pauta.builder()
                .title("Pauta Timestamps")
                .description("Descricao")
                .date(LocalDateTime.of(2026, 5, 1, 10, 0))
                .build();

        Pauta saved = pautaRepository.save(pauta);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        LocalDateTime firstUpdatedAt = saved.getUpdatedAt();

        saved.setDescription("Descricao atualizada");
        Pauta updated = pautaRepository.save(saved);
        entityManager.flush();

        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(updated.getDescription()).isEqualTo("Descricao atualizada");
    }
}
