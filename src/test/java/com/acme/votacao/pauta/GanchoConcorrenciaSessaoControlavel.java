package com.acme.votacao.pauta;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gancho de teste que força sobreposição real entre duas transações concorrentes de
 * {@link SessaoService#abrir}: a primeira thread a chegar aqui (ou seja, que já conseguiu o
 * lock pessimista) fica retida até o teste liberar explicitamente, permitindo ao teste
 * verificar, enquanto ela está retida, que uma segunda chamada concorrente ainda não
 * conseguiu chegar a este mesmo ponto — prova de que o lock bloqueou a segunda transação.
 * Também captura o nível de isolamento efetivo da transação (via
 * {@link TransactionSynchronizationManager}) num {@link AtomicReference}, para o teste
 * assertar na própria thread de teste — assert dentro do gancho lançaria exceção na
 * thread da requisição, virando 500 em vez de falha de teste legível.
 */
public class GanchoConcorrenciaSessaoControlavel implements GanchoConcorrenciaSessao {

    private final AtomicInteger chegadas = new AtomicInteger();
    private final AtomicReference<Integer> nivelIsolamentoObservado = new AtomicReference<>();
    private final AtomicBoolean lancarErroNaProximaChamada = new AtomicBoolean(false);
    private volatile CountDownLatch primeiraChegada = new CountDownLatch(1);
    private volatile CountDownLatch segundaChegada = new CountDownLatch(1);
    private volatile CountDownLatch liberarPrimeira = new CountDownLatch(0);

    /** Reinicia o gancho para um novo teste de concorrência: zera contadores e volta a reter a primeira chegada até liberarPrimeira() ser chamado. */
    public void reset() {
        chegadas.set(0);
        nivelIsolamentoObservado.set(null);
        lancarErroNaProximaChamada.set(false);
        primeiraChegada = new CountDownLatch(1);
        segundaChegada = new CountDownLatch(1);
        liberarPrimeira = new CountDownLatch(1);
    }

    /** Faz a próxima chamada a {@link #aposObterLock} lançar RuntimeException — usado só pelo teste do caminho 500. */
    public void lancarErroNaProximaChamada() {
        lancarErroNaProximaChamada.set(true);
    }

    @Override
    public void aposObterLock(Long pautaId) {
        if (lancarErroNaProximaChamada.compareAndSet(true, false)) {
            throw new RuntimeException("falha forçada pelo teste para exercitar o caminho 500");
        }
        nivelIsolamentoObservado.compareAndSet(null, TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
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

    /** Nível de isolamento (constante de {@link TransactionDefinition}) observado na transação da primeira chegada, ou null se ainda não houve chegada. */
    public Integer nivelIsolamentoObservado() {
        return nivelIsolamentoObservado.get();
    }

    public void liberarPrimeira() {
        liberarPrimeira.countDown();
    }
}
