package com.arcana.backend.character;

import com.arcana.backend.character.model.TipoJogador;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

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

        /** Chaves das perícias com proficiência (ex: "acrobacia", "arcanismo") */
        Map<String, @NotNull(message = "Bônus da perícia não pode ser nulo")
                    @Min(value = 0, message = "Bônus da perícia não pode ser negativo")
                    @Max(value = 20, message = "Bônus da perícia não pode passar de 20") Integer> skills,

        /** Equipamentos em texto livre */
        String equipment,

        /** Magias do personagem */
        List<SpellDto> spells,

        /** Slots de magia por círculo: {"1": 4, "2": 3, ...} */
        Map<String, Integer> spellSlots,

        String backstory,

        String personalityPrompt,

        String avatarUrl,

        @Size(max = 60, message = "Classe não pode ter mais de 60 caracteres")
        String classe,

        @Size(max = 60, message = "Subclasse não pode ter mais de 60 caracteres")
        String subclasse,

        @Min(value = 0, message = "Bônus de proficiência não pode ser negativo")
        @Max(value = 10, message = "Bônus de proficiência não pode passar de 10")
        Integer proficiencyBonus,

        Map<String, @NotNull(message = "Bônus do atributo não pode ser nulo")
                    @Min(value = 0, message = "Bônus do atributo não pode ser negativo")
                    @Max(value = 20, message = "Bônus do atributo não pode passar de 20") Integer> savingThrows,

        @Size(max = 50, message = "A ficha pode ter no máximo 50 itens")
        List<@NotBlank(message = "Item não pode ser vazio")
             @Size(max = 100, message = "Item não pode ter mais de 100 caracteres") String> items

) {
    public record AtributosDto(
            @NotNull @Min(8) @Max(20) Integer forca,
            @NotNull @Min(8) @Max(20) Integer destreza,
            @NotNull @Min(8) @Max(20) Integer constituicao,
            @NotNull @Min(8) @Max(20) Integer inteligencia,
            @NotNull @Min(8) @Max(20) Integer sabedoria,
            @NotNull @Min(8) @Max(20) Integer carisma
    ) {}

    public record SpellDto(
            @NotBlank String name,
            @NotNull @Min(0) @Max(9) Integer level,
            String description
    ) {}
}
