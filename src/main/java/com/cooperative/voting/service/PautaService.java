package com.cooperative.voting.service;

import com.cooperative.voting.entity.Pauta;
import com.cooperative.voting.exception.PautaNotFoundException;
import com.cooperative.voting.repository.PautaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Business logic for creating and retrieving {@link Pauta} agendas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PautaService {

    private final PautaRepository pautaRepository;

    /**
     * Persists a new pauta.
     *
     * @param title       agenda title
     * @param description agenda description
     * @param date        agenda date
     * @return the persisted {@link Pauta}, including generated id and timestamps
     */
    public Pauta createPauta(String title, String description, java.time.LocalDateTime date) {
        log.info("Creating pauta with title: {}", title);
        Pauta pauta = Pauta.builder()
                .title(title)
                .description(description)
                .date(date)
                .build();
        return pautaRepository.save(pauta);
    }

    /**
     * Retrieves a pauta by its identifier.
     *
     * @param id pauta identifier
     * @return the found {@link Pauta}
     * @throws PautaNotFoundException if no pauta exists with the given id
     */
    public Pauta findById(Long id) {
        log.info("Retrieving pauta with id: {}", id);
        return pautaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Pauta with id {} not found", id);
                    return new PautaNotFoundException(id);
                });
    }
}
