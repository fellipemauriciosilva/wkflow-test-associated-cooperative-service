package com.acme.votacao.pauta;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gancho de teste que força sobreposição real entre duas transações concorrentes de
 * {@link SessaoService#abrir}: a primeira thread a chegar aqui (ou seja, que já conseguiu o
 * lock pessimista) fica retida até o teste liberar explicitamente, permitindo ao teste
 * verificar, enquanto ela está retida, que uma segunda chamada concorrente ainda não
 * conseguiu chegar a este mesmo ponto — prova de que o lock bloqueou a segunda transação.
 */
public class GanchoConcorrenciaSessaoControlavel implements GanchoConcorrenciaSessao {

    private final AtomicInteger chegadas = new AtomicInteger();
    private volatile CountDownLatch primeiraChegada = new CountDownLatch(1);
    private volatile CountDownLatch segundaChegada = new CountDownLatch(1);
    private volatile CountDownLatch liberarPrimeira = new CountDownLatch(0);

    /** Fora de um teste de concorrência, o gancho não deve bloquear ninguém: por isso liberarPrimeira começa aberto (contagem zero). */
    public void reset() {
        chegadas.set(0);
        primeiraChegada = new CountDownLatch(1);
        segundaChegada = new CountDownLatch(1);
        liberarPrimeira = new CountDownLatch(1);
    }

    @Override
    public void aposObterLock(Long pautaId) {
        int ordem = chegadas.incrementAndGet();
        if (ordem == 1) {
            primeiraChegada.countDown();
            try {
                boolean liberou = liberarPrimeira.await(10, TimeUnit.SECONDS);
                if (!liberou) {
                    throw new IllegalStateException("teste não liberou a primeira thread a tempo");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        } else {
            segundaChegada.countDown();
        }
    }

    public void aguardarPrimeiraChegada() throws InterruptedException {
        if (!primeiraChegada.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("primeira thread não chegou ao gancho a tempo");
        }
    }

    public boolean segundaChegou() {
        return segundaChegada.getCount() == 0;
    }

    public void liberarPrimeira() {
        liberarPrimeira.countDown();
    }
}
