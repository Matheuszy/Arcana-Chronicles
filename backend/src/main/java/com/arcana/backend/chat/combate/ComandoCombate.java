package com.arcana.backend.chat.combate;

import com.arcana.backend.chat.dice.ModoRolagem;

import java.util.List;

public record ComandoCombate(
        List<Long> jogadores,
        List<GrupoDeMonstros> monstros,
        List<String> entradas,
        ModoRolagem modo,
        Boolean somarProficiencia
) {

    public record GrupoDeMonstros(Long personagemId, Integer quantidade, String nome) {}
}
