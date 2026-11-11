package com.arcana.backend.chat.combate;

import com.arcana.backend.chat.dice.ModoRolagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CombateDaMesaTest {

    private static final Long MESTRE = 1L;
    private static final Instant AGORA = Instant.parse("2026-11-10T12:00:00Z");

    private CombateDaMesa daMesa;

    @BeforeEach
    void setUp() {
        daMesa = new CombateDaMesa();
    }

    private Combate combate(String id, Long... jogadores) {
        Combate combate = new Combate(id, daMesa.proximoNomePadrao(), MESTRE, AGORA);
        for (Long jogador : jogadores) {
            combate.adicionarJogador(jogador, "Jogador " + jogador, "Personagem " + jogador, 0, 0);
        }
        return combate;
    }

    private static Iniciativa total(int total) {
        return new Iniciativa(List.of(total), total, 0, 0, total, ModoRolagem.NORMAL);
    }

    private void rolarTodos(String combateId, Long... jogadores) {
        daMesa.alterar(combateId, combate -> {
            for (int i = 0; i < jogadores.length; i++) {
                combate.registrarIniciativa(Combate.idDoJogador(jogadores[i]), total(20 - i));
            }
        });
    }

    @Test
    void deveManterCombatesSimultaneosComNomesPadraoEmSequencia() {
        daMesa.iniciar(combate("a", 20L));
        daMesa.iniciar(combate("b", 30L));

        List<CombateDto> estados = daMesa.estados();
        assertEquals(2, estados.size());
        assertEquals("Combate 1", estados.get(0).nome());
        assertEquals("Combate 2", estados.get(1).nome());
    }

    @Test
    void deveImpedirJogadorEmDoisCombatesAoMesmoTempo() {
        daMesa.iniciar(combate("a", 20L));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> daMesa.iniciar(combate("b", 30L, 20L)));

        assertEquals("Personagem 20 já está em outro combate.", erro.getMessage());
        assertEquals(1, daMesa.estados().size());
    }

    @Test
    void devePermitirAMesmaFichaDeMonstroEmCombatesDiferentes() {
        Combate primeiro = combate("a", 20L);
        primeiro.adicionarMonstro(500L, "Goblin", "Goblin", "Mestre", 2, 0);
        daMesa.iniciar(primeiro);
        Combate segundo = combate("b", 30L);
        segundo.adicionarMonstro(500L, "Goblin", "Goblin", "Mestre", 2, 0);

        daMesa.iniciar(segundo);

        assertEquals(2, daMesa.estados().size());
    }

    @Test
    void deveEncerrarUmCombateSemAfetarOOutro() {
        daMesa.iniciar(combate("a", 20L));
        daMesa.iniciar(combate("b", 30L));

        daMesa.encerrar("a");

        assertEquals(List.of("b"), daMesa.estados().stream().map(CombateDto::id).toList());
    }

    @Test
    void deveEncerrarSoOCombateQueFicouSemJogadores() {
        daMesa.iniciar(combate("a", 20L));
        daMesa.iniciar(combate("b", 30L, 40L));

        daMesa.removerJogador(20L);
        daMesa.removerJogador(30L);

        assertEquals(List.of("b"), daMesa.estados().stream().map(CombateDto::id).toList());
    }

    @Test
    void deveExigirEscolhaDoCombateQuandoHaMaisDeUm() {
        daMesa.iniciar(combate("a", 20L));
        daMesa.iniciar(combate("b", 30L));

        assertThrows(IllegalArgumentException.class, () -> daMesa.encerrar(null));
        assertThrows(IllegalStateException.class, () -> daMesa.encerrar("inexistente"));
    }

    @Test
    void deveMostrarAoJogadorSoOProprioCombateCompletoEOsDemaisComoResumo() {
        daMesa.iniciar(combate("a", 20L));
        daMesa.iniciar(combate("b", 30L, 40L));

        CombatesDaMesaDto visao = daMesa.visaoDo(20L, false);

        assertEquals(List.of("a"), visao.completos().stream().map(CombateDto::id).toList());
        assertEquals(1, visao.resumos().size());
        assertEquals("Combate 2", visao.resumos().get(0).nome());
        assertEquals(List.of("Personagem 30", "Personagem 40"), visao.resumos().get(0).jogadores());
    }

    @Test
    void deveMostrarAoMestreTodosOsCombatesCompletos() {
        daMesa.iniciar(combate("a", 20L));
        daMesa.iniciar(combate("b", 30L));

        CombatesDaMesaDto visao = daMesa.visaoDo(MESTRE, true);

        assertEquals(2, visao.completos().size());
        assertTrue(visao.resumos().isEmpty());
    }

    @Test
    void deveLiberarRolagemLivreSoNoTurnoDeQuemEstaEmCombateESempreParaQuemEstaFora() {
        daMesa.iniciar(combate("a", 20L, 30L));
        rolarTodos("a", 20L, 30L);

        assertTrue(daMesa.podeRolarLivremente(20L));
        assertFalse(daMesa.podeRolarLivremente(30L));
        assertTrue(daMesa.podeRolarLivremente(40L));
    }

    @Test
    void deveAplicarARegraDeRolagemLivreDoMestreSoNosCombatesComMonstros() {
        Combate comMonstro = combate("a", 20L);
        comMonstro.adicionarMonstro(500L, "Goblin", "Goblin", "Mestre", 2, 0);
        daMesa.iniciar(comMonstro);

        assertFalse(daMesa.podeRolarLivremente(MESTRE));

        daMesa.alterar("a", combate -> {
            combate.registrarIniciativa(Combate.idDoJogador(20L), total(5));
            combate.registrarIniciativa("monstro-1", total(18));
        });

        assertTrue(daMesa.podeRolarLivremente(MESTRE));
    }

    @Test
    void deveAlterarOCombateDoJogadorSemPrecisarDoIdentificador() {
        daMesa.iniciar(combate("a", 20L));
        daMesa.iniciar(combate("b", 30L));

        CombateDto alterado = daMesa.alterarDoJogador(30L,
                combate -> combate.registrarIniciativa(Combate.idDoJogador(30L), total(12)));

        assertEquals("b", alterado.id());
        assertThrows(IllegalStateException.class, () -> daMesa.alterarDoJogador(99L, combate -> {}));
    }

    @Test
    void deveRecusarAdicionarJogadorQueEstaEmOutroCombate() {
        daMesa.iniciar(combate("a", 20L));
        daMesa.iniciar(combate("b", 30L));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> daMesa.adicionar("b", Map.of(20L, "Lyra"), combate -> combate.adicionarJogador(
                        20L, "Aria", "Lyra", 0, 0)));

        assertEquals("Lyra já está em outro combate.", erro.getMessage());
        assertEquals(1, daMesa.visaoDo(MESTRE, true).completos().get(1).participantes().size());
    }

    @Test
    void deveRecusarAdicionarJogadorQueJaEstaNoMesmoCombate() {
        daMesa.iniciar(combate("a", 20L));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> daMesa.adicionar("a", Map.of(20L, "Lyra"), combate -> {}));

        assertEquals("Lyra já está neste combate.", erro.getMessage());
    }
}
