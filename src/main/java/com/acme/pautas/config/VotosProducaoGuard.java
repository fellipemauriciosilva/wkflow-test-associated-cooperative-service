package com.acme.pautas.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class VotosProducaoGuard implements InitializingBean {

    private static final Set<String> PERFIS_PERMITIDOS = Set.of("dev", "test", "local");

    private final Environment ambiente;
    private final boolean votosHabilitados;

    public VotosProducaoGuard(Environment ambiente,
                              @Value("${pautas.votos.habilitado:false}") boolean votosHabilitados) {
        this.ambiente = ambiente;
        this.votosHabilitados = votosHabilitados;
    }

    @Override
    public void afterPropertiesSet() {
        if (votosHabilitados && !somentePerfisPermitidos()) {
            throw new IllegalStateException(
                    "pautas.votos.habilitado=true só é permitido nos perfis dev, test ou local: "
                            + "voto sem identificação do associado");
        }
    }

    private boolean somentePerfisPermitidos() {
        String[] ativos = ambiente.getActiveProfiles();
        return ativos.length > 0 && Arrays.stream(ativos).allMatch(PERFIS_PERMITIDOS::contains);
    }
}
