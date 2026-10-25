package com.acme.pautas.service;

import com.acme.pautas.RelogioDeTeste;
import com.acme.pautas.domain.OpcaoVoto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Import(RelogioDeTeste.class)
class VotoConcorrenciaTest {

    private static final int THREADS = 8;
    private static final String GRAVADO = "gravado";
    private static final String REJEITADO_PELA_APLICACAO = "rejeitado-aplicacao";
    private static final String REJEITADO_PELA_CONSTRAINT = "rejeitado-constraint";

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
    void votosSimultaneosDoMesmoAssociadoGravamUmSoVoto() throws Exception {
        VotoRepository espiado = mock(VotoRepository.class, delegatesTo(votosRepositorio));
        doAnswer(chamada -> {
            Object resultado = chamada.getMethod().invoke(votosRepositorio, chamada.getArguments());
            Thread.sleep(50);
            return resultado;
        }).when(espiado).existsByPautaIdAndAssociadoId(anyLong(), anyString());
        VotoService servico = new VotoService(pautasRepositorio, sessoesRepositorio, espiado, clock);
        TransactionTemplate transacao = new TransactionTemplate(transacoes);

        Long pautaId = pautas.cadastrar("Pauta voto concorrente", null).getId();
        sessoes.abrir(pautaId, 5);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CyclicBarrier barreira = new CyclicBarrier(THREADS);
        try {
            List<Future<String>> resultados = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                Callable<String> voto = () -> {
                    barreira.await();
                    try {
                        transacao.executeWithoutResult(status ->
                                servico.registrar(pautaId, "a1", OpcaoVoto.SIM));
                        return GRAVADO;
                    } catch (VotoDuplicadoException e) {
                        return e.getCause() == null ? REJEITADO_PELA_APLICACAO : REJEITADO_PELA_CONSTRAINT;
                    }
                };
                resultados.add(executor.submit(voto));
            }

            long gravados = 0;
            long pelaAplicacao = 0;
            long pelaConstraint = 0;
            for (Future<String> resultado : resultados) {
                switch (resultado.get()) {
                    case GRAVADO -> gravados++;
                    case REJEITADO_PELA_APLICACAO -> pelaAplicacao++;
                    default -> pelaConstraint++;
                }
            }

            assertEquals(1, gravados, "exatamente um voto gravado");
            assertEquals(THREADS - 1, pelaAplicacao + pelaConstraint, "demais rejeitados com 409");
            assertEquals(0, pelaConstraint, "lock deve impedir que a constraint seja o único guarda");
            assertEquals(1, votosRepositorio.countByPautaId(pautaId), "contagem final 1");
        } finally {
            executor.shutdownNow();
        }
    }
}
