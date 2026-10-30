package com.arcana.backend.chat.combate;

import com.arcana.backend.chat.dice.ModoRolagem;

import java.util.List;

public record Iniciativa(
        List<Integer> dados,
        int mantido,
        int modificadorDestreza,
        int bonusProficiencia,
        int total,
        ModoRolagem modo,
        boolean somouProficiencia
) {}
