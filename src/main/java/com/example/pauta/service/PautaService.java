package com.example.pauta.service;

import com.example.pauta.entity.Pauta;
import com.example.pauta.exception.PautaNotFoundException;
import com.example.pauta.repository.PautaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PautaService {

    private final PautaRepository pautaRepository;

    @Transactional
    public Pauta create(String title) {
        Pauta pauta = new Pauta();
        pauta.setTitle(title.trim());
        pauta.setCreatedAt(Instant.now());
        Pauta saved = pautaRepository.save(pauta);
        log.info("Pauta created with id {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Pauta findById(Long id) {
        return pautaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Pauta not found with id {}", id);
                    return new PautaNotFoundException(id);
                });
    }
}
