package com.arcana.backend.chat.combate;

import com.arcana.backend.chat.dice.ModoRolagem;

import java.util.List;

public record ComandoCombate(
        List<Long> jogadores,
        ModoRolagem modo,
        Boolean somarProficiencia
) {}
