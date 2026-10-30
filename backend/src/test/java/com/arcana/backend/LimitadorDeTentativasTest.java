package com.arcana.backend;

import com.arcana.backend.config.security.LimitadorDeTentativas;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitadorDeTentativasTest {

    private static final class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-01-01T00:00:00Z");

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private final LimitadorDeTentativas limitador = new LimitadorDeTentativas(3, Duration.ofMinutes(1), relogio);

    @Test
    void deveBloquearQuandoPassaDoMaximoNaJanela() {
        assertTrue(limitador.permitir("10.0.0.1"));
        assertTrue(limitador.permitir("10.0.0.1"));
        assertTrue(limitador.permitir("10.0.0.1"));

        assertFalse(limitador.permitir("10.0.0.1"));
    }

    @Test
    void deveContarCadaChaveSeparadamente() {
        for (int i = 0; i < 3; i++) {
            limitador.permitir("10.0.0.1");
        }

        assertTrue(limitador.permitir("10.0.0.2"));
    }

    @Test
    void deveLiberarDepoisQueAJanelaPassa() {
        for (int i = 0; i < 3; i++) {
            limitador.permitir("10.0.0.1");
        }
        relogio.avancar(Duration.ofMinutes(1).plusSeconds(1));

        assertTrue(limitador.permitir("10.0.0.1"));
    }

    @Test
    void deveLiberarAChaveAoEsquecer() {
        for (int i = 0; i < 3; i++) {
            limitador.permitir("sessao");
        }
        limitador.esquecer("sessao");

        assertTrue(limitador.permitir("sessao"));
    }
}
