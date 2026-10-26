package br.acme.wkflow.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoJson {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer numerosInteirosEstritos() {
        return builder -> builder.postConfigurer(mapper -> {
            mapper.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            mapper.coercionConfigFor(LogicalType.Integer)
                    .setCoercion(CoercionInputShape.String, CoercionAction.Fail);
        });
    }
}
