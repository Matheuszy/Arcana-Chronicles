package com.arcana.backend.chat.combate;

import com.arcana.backend.chat.dice.ModoRolagem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ComandoCombate(
        List<Long> jogadores,
        List<GrupoDeMonstros> monstros,
        List<String> entradas,
        ModoRolagem modo
) {

    public record GrupoDeMonstros(Long personagemId, Integer quantidade, String nome) {}
}
