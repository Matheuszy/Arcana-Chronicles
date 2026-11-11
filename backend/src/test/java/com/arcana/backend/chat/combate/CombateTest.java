package com.arcana.backend.chat.combate;

import com.arcana.backend.chat.dice.ModoRolagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CombateTest {

    private static final Long MESTRE = 1L;

    private Combate combate;

    @BeforeEach
    void setUp() {
        combate = new Combate("c1", "Combate 1", MESTRE, Instant.parse("2026-10-30T12:00:00Z"));
        combate.adicionarJogador(20L, "Aria", "Lyra", 2, 2);
        combate.adicionarJogador(30L, "Brom", "Thorn", 3, 2);
        combate.adicionarJogador(40L, "Cael", "Mira", 1, 2);
    }

    private static Iniciativa total(int total) {
        return new Iniciativa(List.of(total), total, 0, 0, total, ModoRolagem.NORMAL);
    }

    private void todosRolam(int aria, int brom, int cael) {
        combate.registrarIniciativa("20", total(aria));
        combate.registrarIniciativa("30", total(brom));
        combate.registrarIniciativa("40", total(cael));
    }

    private List<String> ordem() {
        return combate.entradasEmOrdem().stream().map(Combate.Entrada::getId).toList();
    }

    @Test
    void deveOrdenarPeloMaiorTotalSoQuandoTodosRolarem() {
        combate.registrarIniciativa("20", total(10));
        combate.registrarIniciativa("30", total(18));

        assertEquals(Combate.Fase.INICIATIVA, combate.getFase());
        assertNull(combate.idDoTurno());

        combate.registrarIniciativa("40", total(14));

        assertEquals(Combate.Fase.EM_ANDAMENTO, combate.getFase());
        assertEquals(List.of("30", "40", "20"), ordem());
        assertEquals("30", combate.idDoTurno());
    }

    @Test
    void deveDesempatarPelaDestrezaMaior() {
        todosRolam(15, 15, 15);

        assertEquals(List.of("30", "20", "40"), ordem());
    }

    @Test
    void deveManterOTurnoDeQuemEstaNaVezAoReordenarPorRerolagem() {
        todosRolam(10, 18, 14);
        combate.passarTurno();
        assertEquals("40", combate.idDoTurno());

        combate.liberarRerolagem(List.of("20"));
        combate.registrarIniciativa("20", total(30));

        assertEquals(List.of("20", "30", "40"), ordem());
        assertEquals("40", combate.idDoTurno());
    }

    @Test
    void deveVoltarAoPrimeiroEAvancarARodadaAoPassarOUltimoTurno() {
        todosRolam(10, 18, 14);

        combate.passarTurno();
        combate.passarTurno();
        assertEquals(1, combate.getRodada());

        combate.passarTurno();

        assertEquals("30", combate.idDoTurno());
        assertEquals(2, combate.getRodada());
    }

    @Test
    void deveRecusarPassarTurnoAntesDaIniciativaCompleta() {
        combate.registrarIniciativa("20", total(10));

        assertThrows(IllegalStateException.class, () -> combate.passarTurno());
    }

    @Test
    void deveRecusarLiberarRerolagemDeQuemNaoParticipa() {
        todosRolam(10, 18, 14);

        assertThrows(IllegalStateException.class, () -> combate.liberarRerolagem(List.of("99")));
    }

    @Test
    void deveIniciarOCombateQuandoOUltimoPendenteSaiDaMesa() {
        combate.registrarIniciativa("20", total(10));
        combate.registrarIniciativa("30", total(18));

        combate.removerJogador(40L);

        assertEquals(Combate.Fase.EM_ANDAMENTO, combate.getFase());
        assertEquals(List.of("30", "20"), ordem());
    }

    @Test
    void devePassarOTurnoAoProximoQuandoQuemEstavaNaVezSai() {
        todosRolam(10, 18, 14);

        combate.removerJogador(30L);

        assertEquals("40", combate.idDoTurno());
        assertEquals(1, combate.getRodada());
    }

    @Test
    void deveContarNovaRodadaQuandoOUltimoDaOrdemSaiNaPropriaVez() {
        todosRolam(10, 18, 14);
        combate.passarTurno();
        combate.passarTurno();

        combate.removerJogador(20L);

        assertEquals("30", combate.idDoTurno());
        assertEquals(2, combate.getRodada());
    }

    @Test
    void deveManterOTurnoQuandoSaiAlguemQueJaTinhaJogado() {
        todosRolam(10, 18, 14);
        combate.passarTurno();

        combate.removerJogador(30L);

        assertEquals("40", combate.idDoTurno());
    }

    @Test
    void devePermitirRolagemLivreSoParaQuemEstaNoTurno() {
        assertFalse(combate.podeRolarLivremente(30L));

        todosRolam(10, 18, 14);

        assertTrue(combate.podeRolarLivremente(30L));
        assertFalse(combate.podeRolarLivremente(20L));
    }

    @Test
    void deveEsperarOsMonstrosRolaremParaFecharAOrdem() {
        combate.adicionarMonstro(500L, "Goblin", "Goblin 1", "Gandalf", 2, 2);
        combate.adicionarMonstro(500L, "Goblin", "Goblin 2", "Gandalf", 2, 2);
        todosRolam(10, 18, 14);

        assertEquals(Combate.Fase.INICIATIVA, combate.getFase());

        combate.registrarIniciativa("monstro-1", total(16));
        combate.registrarIniciativa("monstro-2", total(12));

        assertEquals(List.of("30", "monstro-1", "40", "monstro-2", "20"), ordem());
    }

    @Test
    void deveEntregarOTurnoDoMonstroAoMestre() {
        combate.adicionarMonstro(500L, "Goblin", "Goblin", "Gandalf", 2, 2);
        todosRolam(10, 18, 14);
        combate.registrarIniciativa("monstro-1", total(30));

        assertEquals("monstro-1", combate.idDoTurno());
        assertEquals(MESTRE, combate.controladorDoTurno());
        assertTrue(combate.podeRolarLivremente(MESTRE));
        assertFalse(combate.podeRolarLivremente(30L));
    }

    @Test
    void deveDarUmaEntradaPorMonstroSemCopiarAFicha() {
        combate.adicionarMonstro(500L, "Goblin", "Goblin 1", "Gandalf", 2, 2);
        combate.adicionarMonstro(500L, "Goblin", "Goblin 2", "Gandalf", 2, 2);

        Combate.Entrada primeiro = combate.entrada("monstro-1");
        Combate.Entrada segundo = combate.entrada("monstro-2");

        assertEquals(500L, primeiro.getPersonagemId());
        assertEquals(500L, segundo.getPersonagemId());
        assertEquals("Goblin", primeiro.getGrupo());
        assertEquals(Combate.TipoEntrada.MONSTRO, primeiro.getTipo());
        assertNotEquals(primeiro.getId(), segundo.getId());
    }

    @Test
    void deveEncerrarOCombateQuandoSoSobramMonstros() {
        combate.adicionarMonstro(500L, "Goblin", "Goblin", "Gandalf", 2, 2);
        CombateDaMesa daMesa = new CombateDaMesa();
        daMesa.iniciar(combate);

        daMesa.removerJogador(20L);
        daMesa.removerJogador(30L);
        assertEquals(1, daMesa.estados().size());

        daMesa.removerJogador(40L);

        assertTrue(daMesa.estados().isEmpty());
    }

    @Test
    void deveDeixarQuemEntraNaFaseDeIniciativaRolarNaHora() {
        combate.adicionarJogador(50L, "Dax", "Kael", 0, 0);

        Combate.Entrada dax = combate.entrada("50");
        assertFalse(dax.isAguardandoFimDoTurno());
        assertDoesNotThrow(() -> combate.registrarIniciativa("50", total(12)));
    }

    @Test
    void deveSegurarARolagemDeQuemEntraNoCombateEmAndamentoAteOFimDoTurnoAtual() {
        todosRolam(20, 15, 10);
        combate.adicionarJogador(50L, "Dax", "Kael", 0, 0);

        assertTrue(combate.entrada("50").isAguardandoFimDoTurno());
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> combate.registrarIniciativa("50", total(12)));
        assertEquals("Aguarde o fim do turno atual para rolar a iniciativa.", erro.getMessage());

        combate.passarTurno();

        assertFalse(combate.entrada("50").isAguardandoFimDoTurno());
        assertDoesNotThrow(() -> combate.registrarIniciativa("50", total(12)));
    }

    @Test
    void deveManterOCombateAndandoEnquantoQuemEntrouNaoRola() {
        todosRolam(20, 15, 10);
        combate.adicionarJogador(50L, "Dax", "Kael", 0, 0);

        combate.passarTurno();
        combate.passarTurno();
        combate.passarTurno();

        assertEquals(Combate.Fase.EM_ANDAMENTO, combate.getFase());
        assertEquals(2, combate.getRodada());
        assertEquals("20", combate.idDoTurno());
        assertEquals(List.of("20", "30", "40", "50"), ordem());
    }

    @Test
    void deveAgirNestaRodadaQuandoAPosicaoDeQuemEntrouAindaNaoPassou() {
        todosRolam(20, 15, 10);
        combate.adicionarJogador(50L, "Dax", "Kael", 0, 0);
        combate.passarTurno();

        combate.registrarIniciativa("50", total(12));

        assertEquals(List.of("20", "30", "50", "40"), ordem());
        assertEquals("30", combate.idDoTurno());
        combate.passarTurno();
        assertEquals("50", combate.idDoTurno());
        assertEquals(1, combate.getRodada());
    }

    @Test
    void deveAgirSoNaProximaRodadaQuandoAPosicaoDeQuemEntrouJaPassou() {
        todosRolam(20, 15, 10);
        combate.adicionarJogador(50L, "Dax", "Kael", 0, 0);
        combate.passarTurno();

        combate.registrarIniciativa("50", total(18));

        assertEquals(List.of("20", "50", "30", "40"), ordem());
        assertEquals("30", combate.idDoTurno());
        combate.passarTurno();
        assertEquals("40", combate.idDoTurno());
        combate.passarTurno();
        assertEquals("20", combate.idDoTurno());
        assertEquals(2, combate.getRodada());
        combate.passarTurno();
        assertEquals("50", combate.idDoTurno());
    }

    @Test
    void deveRecusarJogadorQueJaEstaNoCombate() {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> combate.adicionarJogador(20L, "Aria", "Lyra", 2, 0));

        assertEquals("Lyra já está neste combate.", erro.getMessage());
    }

    @Test
    void deveRemoverQuemEntrouSemRolarSemMexerNoTurno() {
        todosRolam(20, 15, 10);
        combate.passarTurno();
        combate.adicionarJogador(50L, "Dax", "Kael", 0, 0);

        combate.removerJogador(50L);

        assertEquals("30", combate.idDoTurno());
        assertEquals(List.of("20", "30", "40"), ordem());
    }
}
