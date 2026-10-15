package br.coop.assembly.service;

import br.coop.assembly.entity.Pauta;
import br.coop.assembly.exception.PautaNotFoundException;
import br.coop.assembly.exception.PautaValidationException;
import br.coop.assembly.repository.PautaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class PautaService {
    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Pauta createPauta(String title, String description) {
        log.debug("Creating pauta with title: {}", title);
        validateTitle(title);
        validateDescription(description);

        Pauta pauta = new Pauta();
        pauta.setTitle(title);
        pauta.setDescription(description);
        pauta.setIsDeleted(false);

        Pauta saved = repository.save(pauta);
        log.debug("Pauta created with id: {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Pauta getPautaById(Long id) {
        log.debug("Retrieving pauta with id: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Pauta not found with id: {}", id);
                    return new PautaNotFoundException(id);
                });
    }

    @Transactional(readOnly = true)
    public List<Pauta> getAllPautas() {
        log.debug("Retrieving all pautas");
        return repository.findAll();
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new PautaValidationException("Título é obrigatório");
        }
        if (title.length() < 3 || title.length() > 255) {
            throw new PautaValidationException("Título deve ter entre 3 e 255 caracteres");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > 1000) {
            throw new PautaValidationException("Descrição não pode exceder 1000 caracteres");
        }
    }
}
