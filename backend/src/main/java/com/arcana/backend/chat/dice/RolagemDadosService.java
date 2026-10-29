package com.arcana.backend.chat.dice;

import com.arcana.backend.chat.model.ChatMessage.DiceRollResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

@Service
public class RolagemDadosService {

    public static final Set<Integer> LADOS_PERMITIDOS = Set.of(4, 6, 8, 10, 12, 20, 100);
    public static final int QUANTIDADE_MAXIMA = 20;
    public static final int MODIFICADOR_MAXIMO = 20;

    private final RandomGenerator random;

    @Autowired
    public RolagemDadosService() {
        this(new SecureRandom());
    }

    public RolagemDadosService(RandomGenerator random) {
        this.random = random;
    }

    public DiceRollResult rolar(PedidoRolagem pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido de rolagem ausente.");
        }

        int lados = valorOuPadrao(pedido.lados(), 0);
        int quantidade = valorOuPadrao(pedido.quantidade(), 1);
        int modificador = valorOuPadrao(pedido.modificador(), 0);
        ModoRolagem modo = pedido.modo() != null ? pedido.modo() : ModoRolagem.NORMAL;

        validar(lados, quantidade, modificador, modo);

        List<Integer> rolagens;
        int mantido;

        if (modo == ModoRolagem.NORMAL) {
            rolagens = IntStream.range(0, quantidade).map(i -> rolarDado(lados)).boxed().toList();
            mantido = rolagens.stream().mapToInt(Integer::intValue).sum();
        } else {
            rolagens = List.of(rolarDado(20), rolarDado(20));
            mantido = modo == ModoRolagem.VANTAGEM
                    ? Math.max(rolagens.get(0), rolagens.get(1))
                    : Math.min(rolagens.get(0), rolagens.get(1));
        }

        boolean dadoUnicoD20 = lados == 20 && (quantidade == 1 || modo != ModoRolagem.NORMAL);

        DiceRollResult resultado = new DiceRollResult();
        resultado.setExpression(montarExpressao(lados, quantidade, modificador));
        resultado.setSides(lados);
        resultado.setQuantity(quantidade);
        resultado.setMode(modo.name());
        resultado.setRolls(rolagens);
        resultado.setKept(mantido);
        resultado.setModifier(modificador);
        resultado.setTotal(mantido + modificador);
        resultado.setCritical(dadoUnicoD20 && mantido == 20);
        resultado.setFumble(dadoUnicoD20 && mantido == 1);
        return resultado;
    }

    public String descrever(DiceRollResult resultado) {
        String sufixo = switch (ModoRolagem.valueOf(resultado.getMode())) {
            case VANTAGEM -> " com vantagem";
            case DESVANTAGEM -> " com desvantagem";
            case NORMAL -> "";
        };
        return "rolou " + resultado.getExpression() + sufixo;
    }

    private void validar(int lados, int quantidade, int modificador, ModoRolagem modo) {
        if (!LADOS_PERMITIDOS.contains(lados)) {
            throw new IllegalArgumentException("Dado inválido: use d4, d6, d8, d10, d12, d20 ou d100.");
        }
        if (quantidade < 1 || quantidade > QUANTIDADE_MAXIMA) {
            throw new IllegalArgumentException("Quantidade de dados deve estar entre 1 e " + QUANTIDADE_MAXIMA + ".");
        }
        if (Math.abs(modificador) > MODIFICADOR_MAXIMO) {
            throw new IllegalArgumentException("Modificador deve estar entre -" + MODIFICADOR_MAXIMO + " e +" + MODIFICADOR_MAXIMO + ".");
        }
        if (modo != ModoRolagem.NORMAL && (lados != 20 || quantidade != 1)) {
            throw new IllegalArgumentException("Vantagem e desvantagem só podem ser usadas com 1d20.");
        }
    }

    private int rolarDado(int lados) {
        return random.nextInt(lados) + 1;
    }

    private String montarExpressao(int lados, int quantidade, int modificador) {
        String base = quantidade + "d" + lados;
        if (modificador > 0) return base + "+" + modificador;
        if (modificador < 0) return base + modificador;
        return base;
    }

    private int valorOuPadrao(Integer valor, int padrao) {
        return valor != null ? valor : padrao;
    }
}
