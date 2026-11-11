package com.arcana.backend.chat.desafio;

import com.arcana.backend.chat.model.ChatMessage.DiceRollResult;

import java.util.List;

public record RolagemDeDesafio(
        int tentativa,
        EscolhaTeste escolha,
        String descricaoEscolha,
        DiceRollResult dados,
        List<Ajuda> ajudas,
        int total,
        boolean confirmada,
        boolean sucesso
) {

    public RolagemDeDesafio {
        ajudas = List.copyOf(ajudas);
    }

    public static RolagemDeDesafio pendente(int tentativa, EscolhaTeste escolha, DiceRollResult dados) {
        return new RolagemDeDesafio(tentativa, escolha, escolha.descricao(), dados, List.of(), dados.getTotal(),
                false, false);
    }

    RolagemDeDesafio confirmar(List<Ajuda> ajudasDaTentativa, int cd) {
        int totalFinal = dados.getTotal() + ajudasDaTentativa.stream().mapToInt(Ajuda::efeitoNoTotal).sum();
        return new RolagemDeDesafio(tentativa, escolha, descricaoEscolha, dados, ajudasDaTentativa, totalFinal,
                true, totalFinal >= cd);
    }
}
