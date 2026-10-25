package br.coop.pautas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ConfiguracaoPadraoTest {

    @Test
    void configuracaoBasePastaSchemaSemCriarNemApagarTabelas() throws Exception {
        PropertySource<?> base = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .get(0);

        assertThat(base.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(base.getProperty("spring.datasource.url")).isNull();
    }
}
