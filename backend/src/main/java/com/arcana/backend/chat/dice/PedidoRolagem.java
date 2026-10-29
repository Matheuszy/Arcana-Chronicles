package com.arcana.backend.chat.dice;

public record PedidoRolagem(
        Integer lados,
        Integer quantidade,
        Integer modificador,
        ModoRolagem modo
) {}
