package com.cooperative.associated.infrastructure.persistence;

import com.cooperative.associated.domain.Associate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssociateMapperTest {

    private final AssociateMapper mapper = new AssociateMapper();

    @Test
    void shouldMapDomainToJpaEntity() {
        Associate associate = Associate.create("John Doe", "12345678900", "john@example.com");

        AssociateJpaEntity entity = mapper.toJpaEntity(associate);

        assertThat(entity.getId()).isEqualTo(associate.getId());
        assertThat(entity.getName()).isEqualTo(associate.getName());
        assertThat(entity.getDocument()).isEqualTo(associate.getDocument());
        assertThat(entity.getEmail()).isEqualTo(associate.getEmail());
        assertThat(entity.getCreatedAt()).isEqualTo(associate.getCreatedAt());
    }

    @Test
    void shouldMapJpaEntityToDomain() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        AssociateJpaEntity entity =
                new AssociateJpaEntity(id, "Jane Doe", "98765432100", "jane@example.com", createdAt);

        Associate associate = mapper.toDomain(entity);

        assertThat(associate.getId()).isEqualTo(id);
        assertThat(associate.getName()).isEqualTo("Jane Doe");
        assertThat(associate.getDocument()).isEqualTo("98765432100");
        assertThat(associate.getEmail()).isEqualTo("jane@example.com");
        assertThat(associate.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldMapJpaEntityWithNullEmailToDomain() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        AssociateJpaEntity entity =
                new AssociateJpaEntity(id, "No Email", "11122233344", null, createdAt);

        Associate associate = mapper.toDomain(entity);

        assertThat(associate.getEmail()).isNull();
    }

    @Test
    void shouldRoundTripDomainToJpaAndBack() {
        Associate original = Associate.create("Round Trip", "55566677788", "round@example.com");

        Associate result = mapper.toDomain(mapper.toJpaEntity(original));

        assertThat(result).isEqualTo(original);
        assertThat(result.getName()).isEqualTo(original.getName());
        assertThat(result.getDocument()).isEqualTo(original.getDocument());
        assertThat(result.getEmail()).isEqualTo(original.getEmail());
        assertThat(result.getCreatedAt()).isEqualTo(original.getCreatedAt());
    }
}
