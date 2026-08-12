package com.arcana.backend.character;

import com.arcana.backend.character.model.TipoJogador;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PersonagemRequestDto(

        @NotBlank(message = "Nome do personagem não pode ser vazio")
        @Size(max = 100, message = "Nome do personagem não pode ter mais de 100 caracteres")
        String name,

        @NotNull(message = "Tipo do personagem não pode ser nulo")
        TipoJogador kind,

        @NotNull(message = "Nível não pode ser nulo")
        @Min(value = 1, message = "Nível mínimo é 1")
        Integer level,

        @NotNull(message = "HP máximo não pode ser nulo")
        @Min(value = 1, message = "HP máximo mínimo é 1")
        Integer hpMax,

        @NotNull(message = "HP atual não pode ser nulo")
        @Min(value = 0, message = "HP atual não pode ser negativo")
        Integer hpCurrent,

        @NotNull(message = "Classe de armadura não pode ser nula")
        @Min(value = 0, message = "Classe de armadura não pode ser negativa")
        Integer armorClass,

        @NotNull(message = "Atributos não podem ser nulos")
        @Valid
        AtributosDto attributes,

        String backstory,

        // Usado pela IA para NPCs, Monstros e Bosses — opcional para PLAYER
        String personalityPrompt,

        String avatarUrl

) {
    public record AtributosDto(
            @NotNull @Min(1) @Max(30) Integer forca,
            @NotNull @Min(1) @Max(30) Integer destreza,
            @NotNull @Min(1) @Max(30) Integer constituicao,
            @NotNull @Min(1) @Max(30) Integer inteligencia,
            @NotNull @Min(1) @Max(30) Integer sabedoria,
            @NotNull @Min(1) @Max(30) Integer carisma
    ) {}
}
