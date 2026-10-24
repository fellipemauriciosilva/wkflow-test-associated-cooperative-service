package com.acme.pautas.service;

import com.acme.pautas.RelogioDeTeste;
import com.acme.pautas.repository.SessaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(RelogioDeTeste.class)
class SessaoConcorrenciaTest {

    private static final int THREADS = 8;

    @Autowired
    private PautaService pautas;

    @Autowired
    private SessaoService sessoes;

    @Autowired
    private SessaoRepository repositorio;

    @Test
    void aberturasSimultaneasDeixamUmaSessaoAtiva() throws Exception {
        Long pautaId = pautas.cadastrar("Pauta concorrente", null).getId();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CyclicBarrier barreira = new CyclicBarrier(THREADS);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                Callable<Boolean> abertura = () -> {
                    barreira.await();
                    try {
                        sessoes.abrir(pautaId, 5);
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
