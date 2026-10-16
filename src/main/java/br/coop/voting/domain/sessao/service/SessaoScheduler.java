package br.coop.voting.domain.sessao.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SessaoScheduler {

    private final SessaoService sessaoService;

    public SessaoScheduler(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @Scheduled(fixedDelay = 30000)
    public void verificarExpiracoes() {
        try {
            sessaoService.verificarExpiracoes();
        } catch (Exception e) {
            log.error("Erro ao verificar expiração de sessões", e);
        }
    }
}
