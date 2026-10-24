package com.acme.pautas.service;

import com.acme.pautas.RelogioDeTeste;
import com.acme.pautas.repository.PautaRepository;
import com.acme.pautas.repository.SessaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Import(RelogioDeTeste.class)
class SessaoConcorrenciaTest {

    private static final int THREADS = 8;

    @Autowired
    private PautaRepository pautasRepositorio;

    @Autowired
    private SessaoRepository repositorio;

    @Autowired
    private PautaService pautas;

    @Autowired
    private Clock clock;

    @Autowired
    private PlatformTransactionManager transacoes;

    @Test
    void aberturasSimultaneasDeixamUmaSessaoAtiva() throws Exception {
        SessaoRepository espiado = mock(SessaoRepository.class, delegatesTo(repositorio));
        doAnswer(chamada -> {
            Object resultado = chamada.getMethod().invoke(repositorio, chamada.getArguments());
            Thread.sleep(50);
            return resultado;
        }).when(espiado).existsByPautaIdAndFimAfter(anyLong(), any(Instant.class));
        SessaoService servico = new SessaoService(pautasRepositorio, espiado, clock);
        TransactionTemplate transacao = new TransactionTemplate(transacoes);

        Long pautaId = pautas.cadastrar("Pauta concorrente", null).getId();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CyclicBarrier barreira = new CyclicBarrier(THREADS);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                Callable<Boolean> abertura = () -> {
                    barreira.await();
                    try {
                        transacao.executeWithoutResult(status -> servico.abrir(pautaId, 5));
                        return true;
                    } catch (SessaoJaAbertaException e) {
                        return false;
                    }
                };
                resultados.add(executor.submit(abertura));
            }

            long sucessos = 0;
            long conflitos = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get()) {
                    sucessos++;
                } else {
                    conflitos++;
                }
            }

            assertEquals(1, sucessos);
            assertEquals(THREADS - 1, conflitos);
            assertEquals(1, repositorio.countByPautaId(pautaId));
        } finally {
            executor.shutdownNow();
        }
    }
}
