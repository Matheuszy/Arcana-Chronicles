package com.arcana.backend;

import com.arcana.backend.chat.dice.ModoRolagem;
import com.arcana.backend.chat.dice.PedidoRolagem;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.model.ChatMessage.DiceRollResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class RolagemDadosServiceTest {

    private RolagemDadosService serviceComFaces(int... faces) {
        RandomGenerator sequencia = new RandomGenerator() {
            private int indice = 0;

            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int nextInt(int bound) {
                return faces[indice++] - 1;
            }
        };
        return new RolagemDadosService(sequencia);
    }

    @Test
    void deveSomarTodosOsDadosEOModificador() {
        RolagemDadosService service = serviceComFaces(3, 5);

        DiceRollResult resultado = service.rolar(new PedidoRolagem(6, 2, 3, ModoRolagem.NORMAL));

        assertEquals(List.of(3, 5), resultado.getRolls());
        assertEquals(8, resultado.getKept());
        assertEquals(11, resultado.getTotal());
        assertEquals("2d6+3", resultado.getExpression());
    }

    @Test
    void deveAceitarModificadorNegativo() {
        RolagemDadosService service = serviceComFaces(10);

        DiceRollResult resultado = service.rolar(new PedidoRolagem(20, 1, -2, ModoRolagem.NORMAL));

        assertEquals(8, resultado.getTotal());
        assertEquals("1d20-2", resultado.getExpression());
    }

    @Test
    void deveManterOMaiorDadoComVantagem() {
        RolagemDadosService service = serviceComFaces(7, 15);

        DiceRollResult resultado = service.rolar(new PedidoRolagem(20, 1, 2, ModoRolagem.VANTAGEM));

        assertEquals(List.of(7, 15), resultado.getRolls());
        assertEquals(15, resultado.getKept());
        assertEquals(17, resultado.getTotal());
        assertEquals("rolou 1d20+2 com vantagem", service.descrever(resultado));
    }

    @Test
    void deveManterOMenorDadoComDesvantagem() {
        RolagemDadosService service = serviceComFaces(7, 15);

        DiceRollResult resultado = service.rolar(new PedidoRolagem(20, 1, 0, ModoRolagem.DESVANTAGEM));

        assertEquals(7, resultado.getKept());
        assertEquals(7, resultado.getTotal());
    }

    @Test
    void deveRejeitarVantagemForaDeUmD20() {
        RolagemDadosService service = serviceComFaces();

        assertThrows(IllegalArgumentException.class,
                () -> service.rolar(new PedidoRolagem(6, 1, 0, ModoRolagem.VANTAGEM)));
        assertThrows(IllegalArgumentException.class,
                () -> service.rolar(new PedidoRolagem(20, 2, 0, ModoRolagem.VANTAGEM)));
    }

    @Test
    void deveRejeitarDadoComLadosInvalidos() {
        RolagemDadosService service = serviceComFaces();

        assertThrows(IllegalArgumentException.class,
                () -> service.rolar(new PedidoRolagem(7, 1, 0, ModoRolagem.NORMAL)));
    }

    @Test
    void deveRejeitarQuantidadeForaDoLimite() {
        RolagemDadosService service = serviceComFaces();

        assertThrows(IllegalArgumentException.class,
                () -> service.rolar(new PedidoRolagem(6, 0, 0, ModoRolagem.NORMAL)));
        assertThrows(IllegalArgumentException.class,
                () -> service.rolar(new PedidoRolagem(6, 21, 0, ModoRolagem.NORMAL)));
    }

    @Test
    void deveRejeitarModificadorForaDoLimite() {
        RolagemDadosService service = serviceComFaces();

        assertThrows(IllegalArgumentException.class,
                () -> service.rolar(new PedidoRolagem(20, 1, 21, ModoRolagem.NORMAL)));
        assertThrows(IllegalArgumentException.class,
                () -> service.rolar(new PedidoRolagem(20, 1, -21, ModoRolagem.NORMAL)));
    }

    @Test
    void deveMarcarCriticoEFalhaApenasEmD20() {
        assertTrue(serviceComFaces(20).rolar(new PedidoRolagem(20, 1, 0, ModoRolagem.NORMAL)).isCritical());
        assertTrue(serviceComFaces(1).rolar(new PedidoRolagem(20, 1, 0, ModoRolagem.NORMAL)).isFumble());
        assertFalse(serviceComFaces(1).rolar(new PedidoRolagem(6, 1, 0, ModoRolagem.NORMAL)).isFumble());
        assertFalse(serviceComFaces(10, 10).rolar(new PedidoRolagem(20, 2, 0, ModoRolagem.NORMAL)).isCritical());
    }

    @Test
    void deveRejeitarPedidoAusente() {
        assertThrows(IllegalArgumentException.class, () -> serviceComFaces().rolar(null));
    }
}
