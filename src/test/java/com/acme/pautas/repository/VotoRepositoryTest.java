package com.acme.pautas.repository;

import com.acme.pautas.domain.OpcaoVoto;
import com.acme.pautas.domain.Voto;
import com.acme.pautas.service.PautaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class VotoRepositoryTest {

    @Autowired
    private VotoRepository votos;

    @Autowired
    private PautaService pautas;

    @Test
    void constraintImpedeDoisVotosDoMesmoAssociadoNaMesmaPauta() {
        Long pautaId = pautas.cadastrar("Pauta constraint de voto", null).getId();
        votos.saveAndFlush(new Voto(pautaId, "a1", OpcaoVoto.SIM));

        assertThrows(DataIntegrityViolationException.class,
                () -> votos.saveAndFlush(new Voto(pautaId, "a1", OpcaoVoto.NAO)));
    }
}
