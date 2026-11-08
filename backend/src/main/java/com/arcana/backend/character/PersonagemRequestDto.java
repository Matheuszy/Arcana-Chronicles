package com.arcana.backend.character;

import com.arcana.backend.character.model.TipoEquipamento;
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

        @Size(max = 30, message = "A ficha pode ter no máximo 30 armas e armaduras")
        List<@Valid EquipamentoDto> equipamentos,

        /** Magias do personagem */
        List<SpellDto> spells,

        /** Slots de magia por círculo: {"1": 4, "2": 3, ...} */
        Map<String, Integer> spellSlots,

        Map<String, Integer> spellSlotsCurrent,

        String backstory,

        String personalityPrompt,

        String avatarUrl,

        @NotEmpty(message = "A ficha precisa de ao menos uma classe")
        @Size(max = 10, message = "A ficha pode ter no máximo 10 classes")
        List<@Valid ClasseDto> classes,

        @NotNull(message = "Bônus de proficiência é obrigatório")
        @Min(value = 0, message = "Bônus de proficiência não pode ser negativo")
        @Max(value = 10, message = "Bônus de proficiência não pode passar de 10")
        Integer proficiencyBonus,

        Map<String, @NotNull(message = "Bônus do atributo não pode ser nulo")
                    @Min(value = 0, message = "Bônus do atributo não pode ser negativo")
                    @Max(value = 20, message = "Bônus do atributo não pode passar de 20") Integer> savingThrows,

        @Size(max = 50, message = "A ficha pode ter no máximo 50 itens")
        List<@NotBlank(message = "Item não pode ser vazio")
             @Size(max = 100, message = "Item não pode ter mais de 100 caracteres") String> items,

        @Size(max = 5000, message = "Habilidades não podem ter mais de 5000 caracteres")
        String habilidades,

        @NotBlank(message = "Raça é obrigatória")
        @Size(max = 60, message = "Raça não pode ter mais de 60 caracteres")
        String raca,

        @NotBlank(message = "Alinhamento é obrigatório")
        @Size(max = 60, message = "Alinhamento não pode ter mais de 60 caracteres")
        String alinhamento,

        @Size(max = 5000, message = "Habilidades de raça não podem ter mais de 5000 caracteres")
        String habilidadesRaca,

        @Size(max = 5000, message = "Talentos não podem ter mais de 5000 caracteres")
        String talentos,

        @Min(value = 0, message = "Vida temporária não pode ser negativa")
        Integer vidaTemporaria,

        @NotNull(message = "Deslocamento é obrigatório")
        @Min(value = 0, message = "Deslocamento não pode ser negativo")
        @Max(value = 300, message = "Deslocamento não pode passar de 300 metros")
        Integer deslocamento,

        Boolean inspiracao,

        @Min(value = 0, message = "Sucessos nos testes contra a morte devem ser de 0 a 3")
        @Max(value = 3, message = "Sucessos nos testes contra a morte devem ser de 0 a 3")
        Integer sucessosContraMorte,

        @Min(value = 0, message = "Falhas nos testes contra a morte devem ser de 0 a 3")
        @Max(value = 3, message = "Falhas nos testes contra a morte devem ser de 0 a 3")
        Integer falhasContraMorte

) {
    public record ClasseDto(
            @NotBlank(message = "Toda classe precisa de um nome")
            @Size(max = 60, message = "Classe não pode ter mais de 60 caracteres")
            String classe,

            @Size(max = 60, message = "Subclasse não pode ter mais de 60 caracteres")
            String subclasse,

            @NotNull(message = "O nível da classe é obrigatório")
            @Min(value = 1, message = "O nível da classe deve ser de 1 a 20")
            @Max(value = 20, message = "O nível da classe deve ser de 1 a 20")
            Integer nivel
    ) {}

    public record EquipamentoDto(
            @NotNull(message = "Escolha se o equipamento é arma ou armadura")
            TipoEquipamento tipo,

            @NotBlank(message = "Toda arma ou armadura precisa de um nome")
            @Size(max = 100, message = "O nome da arma ou armadura não pode ter mais de 100 caracteres")
            String nome,

            @Pattern(regexp = "([1-9]|1[0-9]|20)d(4|6|8|10|12|20|100)",
                    message = "O dado de dano deve ser como 1d8 ou 2d6")
            String dano,

            String atributo,

            @Min(value = 0, message = "A CA da armadura não pode ser negativa")
            @Max(value = 30, message = "A CA da armadura não pode passar de 30")
            Integer classeArmadura,

            @Size(max = 200, message = "As propriedades não podem ter mais de 200 caracteres")
            String propriedades
    ) {}

    public record AtributosDto(
            @NotNull(message = "Força é obrigatória") @Min(value = 8, message = "Força deve ser no mínimo 8") @Max(value = 20, message = "Força deve ser no máximo 20") Integer forca,
            @NotNull(message = "Destreza é obrigatória") @Min(value = 8, message = "Destreza deve ser no mínimo 8") @Max(value = 20, message = "Destreza deve ser no máximo 20") Integer destreza,
            @NotNull(message = "Constituição é obrigatória") @Min(value = 8, message = "Constituição deve ser no mínimo 8") @Max(value = 20, message = "Constituição deve ser no máximo 20") Integer constituicao,
            @NotNull(message = "Inteligência é obrigatória") @Min(value = 8, message = "Inteligência deve ser no mínimo 8") @Max(value = 20, message = "Inteligência deve ser no máximo 20") Integer inteligencia,
            @NotNull(message = "Sabedoria é obrigatória") @Min(value = 8, message = "Sabedoria deve ser no mínimo 8") @Max(value = 20, message = "Sabedoria deve ser no máximo 20") Integer sabedoria,
            @NotNull(message = "Carisma é obrigatória") @Min(value = 8, message = "Carisma deve ser no mínimo 8") @Max(value = 20, message = "Carisma deve ser no máximo 20") Integer carisma
    ) {}

    public record SpellDto(
            @NotBlank(message = "Toda magia precisa de um nome") String name,
            @NotNull(message = "O círculo da magia é obrigatório") @Min(value = 0, message = "O círculo da magia deve ser de 0 a 9") @Max(value = 9, message = "O círculo da magia deve ser de 0 a 9") Integer level,
            String description
    ) {}
}
