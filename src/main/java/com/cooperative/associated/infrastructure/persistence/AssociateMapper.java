package com.cooperative.associated.infrastructure.persistence;

import com.cooperative.associated.domain.Associate;
import org.springframework.stereotype.Component;

@Component
public class AssociateMapper {

    public AssociateJpaEntity toJpaEntity(Associate associate) {
        AssociateJpaEntity entity = new AssociateJpaEntity();
        entity.setId(associate.getId());
        entity.setName(associate.getName());
        entity.setDocument(associate.getDocument());
        entity.setEmail(associate.getEmail());
        entity.setCreatedAt(associate.getCreatedAt());
        return entity;
    }

    public Associate toDomain(AssociateJpaEntity entity) {
        return Associate.restore(
                entity.getId(),
                entity.getName(),
                entity.getDocument(),
                entity.getEmail(),
                entity.getCreatedAt()
        );
    }
}
