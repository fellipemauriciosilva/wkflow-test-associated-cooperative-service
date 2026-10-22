package com.cooperativa.votacao.infrastructure.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Por padrao o Jackson trunca silenciosamente um JSON float (ex. 1.5) para Integer (1),
    // sem erro. Isso mascararia "duracao nao inteira" como valida. Forca falha nessa coercao.
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer rejeitarCoercaoDeFloatParaInteger() {
        return builder -> builder.postConfigurer(mapper -> mapper.coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail));
    }
}
