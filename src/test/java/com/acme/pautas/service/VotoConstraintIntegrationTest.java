package com.acme.pautas.service;

import com.acme.pautas.RelogioDeTeste;
import com.acme.pautas.domain.OpcaoVoto;
import com.acme.pautas.domain.Voto;
import com.acme.pautas.repository.PautaRepository;
import com.acme.pautas.repository.SessaoRepository;
import com.acme.pautas.repository.VotoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Import(RelogioDeTeste.class)
class VotoConstraintIntegrationTest {

    @Autowired
    private PautaRepository pautasRepositorio;

    @Autowired
    private SessaoRepository sessoesRepositorio;

    @Autowired
    private VotoRepository votosRepositorio;

    @Autowired
    private PautaService pautas;

    @Autowired
    private SessaoService sessoes;

    @Autowired
    private Clock clock;

    @Autowired
    private PlatformTransactionManager transacoes;

    @Test
    void constraintUnicaNoBancoRespondeDuplicidadeQuandoChecagemDaAplicacaoFalha() {
        Long pautaId = pautas.cadastrar("Pauta constraint", null).getId();
        sessoes.abrir(pautaId, 5);
        votosRepositorio.saveAndFlush(new Voto(pautaId, "a1", OpcaoVoto.SIM));

        VotoRepository semChecagem = mock(VotoRepository.class, delegatesTo(votosRepositorio));
        doReturn(false).when(semChecagem).existsByPautaIdAndAssociadoId(anyLong(), anyString());
        VotoService servico = new VotoService(pautasRepositorio, sessoesRepositorio, semChecagem, clock);

        TransactionTemplate transacao = new TransactionTemplate(transacoes);
        assertThrows(VotoDuplicadoException.class,
                () -> transacao.executeWithoutResult(status -> servico.registrar(pautaId, "a1", OpcaoVoto.NAO)));
        assertEquals(1, votosRepositorio.countByPautaId(pautaId));
    }
}
