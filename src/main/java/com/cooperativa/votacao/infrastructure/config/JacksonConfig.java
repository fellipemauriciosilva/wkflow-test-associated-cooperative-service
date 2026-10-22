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
    // Por padrao o Jackson tambem coage String->Integer (ex. "5" vira 5): decisao de produto
    // (review, item 11) e rejeitar, pois duracaoMinutos e um campo numerico no contrato —
    // aceitar string seria inconsistente com "abc" e "1.5" ja darem 400.
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer rejeitarCoercaoParaInteger() {
        return builder -> builder.postConfigurer(mapper -> mapper.coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.String, CoercionAction.Fail));
    }
}
