package com.arcana.backend.chat.desafio;

import com.arcana.backend.chat.dice.ModoRolagem;

import java.util.List;

public record NovoDesafio(Integer cd, String descricao, List<Convocacao> jogadores) {

    public record Convocacao(Long jogadorId, ModoRolagem modo) {}
}
