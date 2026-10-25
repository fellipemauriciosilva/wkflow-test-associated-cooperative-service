package com.acme.pautas.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class VotosProducaoGuard implements InitializingBean {

    private final Environment ambiente;
    private final boolean votosHabilitados;

    public VotosProducaoGuard(Environment ambiente,
                              @Value("${pautas.votos.habilitado:false}") boolean votosHabilitados) {
        this.ambiente = ambiente;
        this.votosHabilitados = votosHabilitados;
    }

    @Override
    public void afterPropertiesSet() {
        if (votosHabilitados && ambiente.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "pautas.votos.habilitado=true proibido no perfil prod: voto sem identificação do associado");
        }
    }
}
