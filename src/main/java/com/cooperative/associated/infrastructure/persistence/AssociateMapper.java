package com.cooperative.associated.infrastructure.persistence;

import com.cooperative.associated.domain.Associate;
import org.springframework.stereotype.Component;

@Component
public class AssociateMapper {

    public AssociateJpaEntity toJpaEntity(Associate associate) {
        return new AssociateJpaEntity(
                associate.getId(),
                associate.getName(),
                associate.getDocument(),
                associate.getEmail(),
                associate.getCreatedAt()
        );
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
