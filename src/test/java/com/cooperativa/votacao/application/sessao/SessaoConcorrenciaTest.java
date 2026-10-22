package com.cooperativa.votacao.application.sessao;

import static org.assertj.core.api.Assertions.assertThat;

import com.cooperativa.votacao.application.pauta.PautaService;
import com.cooperativa.votacao.domain.sessao.SessaoJaAbertaException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sem @MockBean Clock: usa o Clock real do contexto (ClockConfig.systemUTC) e threads reais
 * contra o H2 de verdade, porque a race do 409 (lock PESSIMISTIC_WRITE em
 * SessaoService.abrir) so se prova com uma transacao concorrente de fato bloqueando outra —
 * MockMvc sequencial ou mocks de repositorio nao exercitam o lock do banco.
 */
@SpringBootTest
class SessaoConcorrenciaTest {

    @Autowired
    private SessaoService sessaoService;

    @Autowired
    private PautaService pautaService;

    @Test
    @DisplayName("N threads abrindo sessao para a mesma pauta: so uma tem sucesso, as demais recebem 409")
    void abrir_comThreadsConcorrentes_apenasUmaSessaoAberta() throws InterruptedException {
        var pauta = pautaService.criar("Eleicao de diretoria " + System.nanoTime(), null);
        var numeroThreads = 10;

        var executor = Executors.newFixedThreadPool(numeroThreads);
        var prontas = new CountDownLatch(numeroThreads);
        var partida = new CountDownLatch(1);
        var terminadas = new CountDownLatch(numeroThreads);
        var sucessos = new AtomicInteger();
        var conflitos = new AtomicInteger();
        var outrosErros = new ArrayList<Throwable>();

        List<Runnable> tarefas = new ArrayList<>();
        for (int i = 0; i < numeroThreads; i++) {
            tarefas.add(() -> {
                prontas.countDown();
                try {
                    partida.await();
                    sessaoService.abrir(pauta.getId(), 60);
                    sucessos.incrementAndGet();
                } catch (SessaoJaAbertaException ex) {
                    conflitos.incrementAndGet();
                } catch (Exception ex) {
                    synchronized (outrosErros) {
                        outrosErros.add(ex);
                    }
                } finally {
                    terminadas.countDown();
                }
            });
        }

        tarefas.forEach(executor::execute);
        prontas.await();
        partida.countDown();
        terminadas.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(outrosErros).isEmpty();
        assertThat(sucessos.get()).isEqualTo(1);
        assertThat(conflitos.get()).isEqualTo(numeroThreads - 1);
    }
}
