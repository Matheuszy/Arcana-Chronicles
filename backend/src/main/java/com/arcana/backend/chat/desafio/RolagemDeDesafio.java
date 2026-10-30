package com.arcana.backend.chat.desafio;

import com.arcana.backend.chat.model.ChatMessage.DiceRollResult;

public record RolagemDeDesafio(
        int tentativa,
        EscolhaTeste escolha,
        String descricaoEscolha,
        DiceRollResult dados,
        int total,
        boolean sucesso
) {}
