package com.acme.votacao.pauta;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gancho de teste que força duas chamadas concorrentes de {@link VotoService#votar} a
 * chegarem ao insert ao mesmo tempo: a primeira a chegar fica retida até a segunda também
 * chegar, garantindo que ambas tentem inserir antes de qualquer commit — prova real de que
 * é a constraint UNIQUE (pauta_id, associado_id) do banco, não a checagem otimista anterior,
 * quem impede o duplicado sob concorrência. Desarmado por padrão (passa direto): como o bean
 * é singleton compartilhado por todos os métodos de teste da classe, barreira sempre ativa
 * bloquearia indefinidamente qualquer chamada de voto fora do teste de concorrência.
 */
public class GanchoConcorrenciaVotoControlavel implements GanchoConcorrenciaVoto {

    private final AtomicBoolean armado = new AtomicBoolean(false);
    private volatile CountDownLatch ambasChegaram = new CountDownLatch(2);

    public void ativarBarreira() {
        ambasChegaram = new CountDownLatch(2);
        armado.set(true);
    }

    @Override
    public void antesDeSalvar(Long pautaId, String associadoId) {
        if (!armado.get()) {
            return;
        }
        ambasChegaram.countDown();
        try {
            boolean ambasChegaramATempo = ambasChegaram.await(10, TimeUnit.SECONDS);
            armado.set(false);
            if (!ambasChegaramATempo) {
                throw new IllegalStateException("as duas chamadas concorrentes não chegaram a tempo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
