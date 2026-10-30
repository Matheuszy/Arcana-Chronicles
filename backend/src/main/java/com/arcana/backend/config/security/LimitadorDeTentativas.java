package com.arcana.backend.config.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LimitadorDeTentativas {

    private static final int CHAVES_PARA_LIMPEZA = 10_000;

    private final int maximo;
    private final Duration janela;
    private final Clock clock;
    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();

    public LimitadorDeTentativas(int maximo, Duration janela, Clock clock) {
        this.maximo = maximo;
        this.janela = janela;
        this.clock = clock;
    }

    public boolean permitir(String chave) {
        Instant agora = clock.instant();
        if (tentativas.size() > CHAVES_PARA_LIMPEZA) {
            removerExpiradas(agora);
        }
        Deque<Instant> fila = tentativas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (fila) {
            descartarAntigas(fila, agora);
            if (fila.size() >= maximo) {
                return false;
            }
            fila.addLast(agora);
            return true;
        }
    }

    public void esquecer(String chave) {
        tentativas.remove(chave);
    }

    public Duration janela() {
        return janela;
    }

    private void removerExpiradas(Instant agora) {
        tentativas.values().removeIf(fila -> {
            synchronized (fila) {
                descartarAntigas(fila, agora);
                return fila.isEmpty();
            }
        });
    }

    private void descartarAntigas(Deque<Instant> fila, Instant agora) {
        Instant limite = agora.minus(janela);
        while (!fila.isEmpty() && fila.peekFirst().isBefore(limite)) {
            fila.removeFirst();
        }
    }
}
