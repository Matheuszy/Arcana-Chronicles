package com.arcana.backend.chat.combate;

import com.arcana.backend.chat.dice.ModoRolagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CombateTest {

    private Combate combate;

    @BeforeEach
    void setUp() {
        combate = new Combate("c1", 1L, Instant.parse("2026-10-30T12:00:00Z"));
        combate.adicionar(20L, "Aria", "Lyra", 2, 2);
        combate.adicionar(30L, "Brom", "Thorn", 3, 2);
        combate.adicionar(40L, "Cael", "Mira", 1, 2);
    }

    private static Iniciativa total(int total) {
        return new Iniciativa(List.of(total), total, 0, 0, total, ModoRolagem.NORMAL, false);
    }

    private void todosRolam(int aria, int brom, int cael) {
        combate.registrarIniciativa(20L, total(aria));
        combate.registrarIniciativa(30L, total(brom));
        combate.registrarIniciativa(40L, total(cael));
    }

    private List<Long> ordem() {
        return combate.entradasEmOrdem().stream().map(Combate.Entrada::getJogadorId).toList();
    }

    @Test
    void deveOrdenarPeloMaiorTotalSoQuandoTodosRolarem() {
        combate.registrarIniciativa(20L, total(10));
        combate.registrarIniciativa(30L, total(18));

        assertEquals(Combate.Fase.INICIATIVA, combate.getFase());
        assertNull(combate.jogadorDoTurno());

        combate.registrarIniciativa(40L, total(14));

        assertEquals(Combate.Fase.EM_ANDAMENTO, combate.getFase());
        assertEquals(List.of(30L, 40L, 20L), ordem());
        assertEquals(30L, combate.jogadorDoTurno());
    }

    @Test
    void deveDesempatarPelaDestrezaMaior() {
        todosRolam(15, 15, 15);

        assertEquals(List.of(30L, 20L, 40L), ordem());
    }

    @Test
    void deveManterOTurnoDeQuemEstaNaVezAoReordenarPorRerolagem() {
        todosRolam(10, 18, 14);
        combate.passarTurno();
        assertEquals(40L, combate.jogadorDoTurno());

        combate.liberarRerolagem(List.of(20L));
        combate.registrarIniciativa(20L, total(30));

        assertEquals(List.of(20L, 30L, 40L), ordem());
        assertEquals(40L, combate.jogadorDoTurno());
    }

    @Test
    void deveVoltarAoPrimeiroEAvancarARodadaAoPassarOUltimoTurno() {
        todosRolam(10, 18, 14);

        combate.passarTurno();
        combate.passarTurno();
        assertEquals(1, combate.getRodada());

        combate.passarTurno();

        assertEquals(30L, combate.jogadorDoTurno());
        assertEquals(2, combate.getRodada());
    }

    @Test
    void deveRecusarPassarTurnoAntesDaIniciativaCompleta() {
        combate.registrarIniciativa(20L, total(10));

        assertThrows(IllegalStateException.class, () -> combate.passarTurno());
    }

    @Test
    void deveRecusarLiberarRerolagemDeQuemNaoParticipa() {
        todosRolam(10, 18, 14);

        assertThrows(IllegalStateException.class, () -> combate.liberarRerolagem(List.of(99L)));
    }

    @Test
    void deveIniciarOCombateQuandoOUltimoPendenteSaiDaMesa() {
        combate.registrarIniciativa(20L, total(10));
        combate.registrarIniciativa(30L, total(18));

        combate.remover(40L);

        assertEquals(Combate.Fase.EM_ANDAMENTO, combate.getFase());
        assertEquals(List.of(30L, 20L), ordem());
    }

    @Test
    void devePassarOTurnoAoProximoQuandoQuemEstavaNaVezSai() {
        todosRolam(10, 18, 14);

        combate.remover(30L);

        assertEquals(40L, combate.jogadorDoTurno());
        assertEquals(1, combate.getRodada());
    }

    @Test
    void deveContarNovaRodadaQuandoOUltimoDaOrdemSaiNaPropriaVez() {
        todosRolam(10, 18, 14);
        combate.passarTurno();
        combate.passarTurno();

        combate.remover(20L);

        assertEquals(30L, combate.jogadorDoTurno());
        assertEquals(2, combate.getRodada());
    }

    @Test
    void deveManterOTurnoQuandoSaiAlguemQueJaTinhaJogado() {
        todosRolam(10, 18, 14);
        combate.passarTurno();

        combate.remover(30L);

        assertEquals(40L, combate.jogadorDoTurno());
    }

    @Test
    void devePermitirRolagemLivreSoParaQuemEstaNoTurno() {
        assertFalse(combate.podeRolarLivremente(30L));

        todosRolam(10, 18, 14);

        assertTrue(combate.podeRolarLivremente(30L));
        assertFalse(combate.podeRolarLivremente(20L));
    }
}
