package br.coop.associados.votacao.pauta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PautaRepositoryTest {

    @Autowired
    private PautaRepository repository;

    @Test
    void persisteComIdGeradoECamposGravados() {
        Pauta salva = repository.save(new Pauta("Aprovar orçamento", "Orçamento 2027"));

        Pauta lida = repository.findById(salva.getId()).orElseThrow();

        assertThat(salva.getId()).isNotNull();
        assertThat(lida.getTitulo()).isEqualTo("Aprovar orçamento");
        assertThat(lida.getDescricao()).isEqualTo("Orçamento 2027");
    }
}
