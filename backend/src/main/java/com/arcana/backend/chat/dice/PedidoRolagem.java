package com.arcana.backend.chat.dice;

public record PedidoRolagem(
        Integer lados,
        Integer quantidade,
        Integer modificador,
        ModoRolagem modo,
        String descricao
) {
    public PedidoRolagem(Integer lados, Integer quantidade, Integer modificador, ModoRolagem modo) {
        this(lados, quantidade, modificador, modo, null);
    }
}
