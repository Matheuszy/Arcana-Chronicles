package com.arcana.backend.character;

public record ResultadoRolagemDeFichaDto(
        RolagemDeFichaDto rolagem,
        PersonagemResponseDto ficha
) {}
