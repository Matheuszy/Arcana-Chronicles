package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record SecaoAtributosRequestDto(
        @NotNull(message = "HP máximo não pode ser nulo")
        @Min(value = 1, message = "HP máximo mínimo é 1")
        Integer hpMax,

        @NotNull(message = "HP atual não pode ser nulo")
        @Min(value = 0, message = "HP atual não pode ser negativo")
        Integer hpCurrent,

        @Min(value = 0, message = "Vida temporária não pode ser negativa")
        Integer vidaTemporaria,

        @NotNull(message = "Classe de armadura não pode ser nula")
        @Min(value = 0, message = "Classe de armadura não pode ser negativa")
        Integer armorClass,

        @Min(value = 0, message = "A CA adicional não pode ser negativa")
        @Max(value = 20, message = "A CA adicional não pode passar de 20")
        Integer caAdicional,

        @NotNull(message = "Deslocamento é obrigatório")
        @Min(value = 0, message = "Deslocamento não pode ser negativo")
        @Max(value = 300, message = "Deslocamento não pode passar de 300 metros")
        Integer deslocamento,

        @Size(max = 10, message = "A ficha pode ter no máximo 10 deslocamentos adicionais")
        List<PersonagemRequestDto.@Valid DeslocamentoAdicionalDto> deslocamentosAdicionais,

        Boolean inspiracao,

        @Min(value = 0, message = "Sucessos nos testes contra a morte devem ser de 0 a 3")
        @Max(value = 3, message = "Sucessos nos testes contra a morte devem ser de 0 a 3")
        Integer sucessosContraMorte,

        @Min(value = 0, message = "Falhas nos testes contra a morte devem ser de 0 a 3")
        @Max(value = 3, message = "Falhas nos testes contra a morte devem ser de 0 a 3")
        Integer falhasContraMorte,

        @NotNull(message = "Bônus de proficiência é obrigatório")
        @Min(value = 0, message = "Bônus de proficiência não pode ser negativo")
        @Max(value = 10, message = "Bônus de proficiência não pode passar de 10")
        Integer proficiencyBonus,

        @NotNull(message = "Atributos não podem ser nulos")
        @Valid
        PersonagemRequestDto.AtributosDto attributes,

        Map<String, @NotNull(message = "Bônus do atributo não pode ser nulo")
                    @Min(value = 0, message = "Bônus do atributo não pode ser negativo")
                    @Max(value = 20, message = "Bônus do atributo não pode passar de 20") Integer> savingThrows,

        Map<String, @NotNull(message = "Bônus da perícia não pode ser nulo")
                    @Min(value = 0, message = "Bônus da perícia não pode ser negativo")
                    @Max(value = 20, message = "Bônus da perícia não pode passar de 20") Integer> skills
) {
    public static SecaoAtributosRequestDto de(PersonagemRequestDto dto) {
        return new SecaoAtributosRequestDto(dto.hpMax(), dto.hpCurrent(), dto.vidaTemporaria(), dto.armorClass(),
                dto.caAdicional(), dto.deslocamento(), dto.deslocamentosAdicionais(), dto.inspiracao(),
                dto.sucessosContraMorte(), dto.falhasContraMorte(), dto.proficiencyBonus(), dto.attributes(),
                dto.savingThrows(), dto.skills());
    }
}
