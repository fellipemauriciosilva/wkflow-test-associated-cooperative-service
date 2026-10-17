package com.cooperative.voting.repository;

import com.cooperative.voting.entity.Pauta;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for {@link Pauta} persistence.
 */
@Repository
public interface PautaRepository extends CrudRepository<Pauta, Long> {
}
