package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SecaoEquipamentoRequestDto(
        @Size(max = 30, message = "A ficha pode ter no máximo 30 armas e armaduras")
        List<PersonagemRequestDto.@Valid EquipamentoDto> equipamentos,

        @Size(max = 50, message = "A ficha pode ter no máximo 50 itens")
        List<@NotBlank(message = "Item não pode ser vazio")
             @Size(max = 100, message = "Item não pode ter mais de 100 caracteres") String> items,

        @Min(value = 0, message = "Moedas de cobre não podem ser negativas")
        @Max(value = 1000000, message = "Moedas de cobre não podem passar de 1.000.000")
        Integer moedasCobre,

        @Min(value = 0, message = "Moedas de prata não podem ser negativas")
        @Max(value = 1000000, message = "Moedas de prata não podem passar de 1.000.000")
        Integer moedasPrata,

        @Min(value = 0, message = "Moedas de electro não podem ser negativas")
        @Max(value = 1000000, message = "Moedas de electro não podem passar de 1.000.000")
        Integer moedasElectro,

        @Min(value = 0, message = "Moedas de ouro não podem ser negativas")
        @Max(value = 1000000, message = "Moedas de ouro não podem passar de 1.000.000")
        Integer moedasOuro,

        @Min(value = 0, message = "Moedas de platina não podem ser negativas")
        @Max(value = 1000000, message = "Moedas de platina não podem passar de 1.000.000")
        Integer moedasPlatina
) {
    public static SecaoEquipamentoRequestDto de(PersonagemRequestDto dto) {
        return new SecaoEquipamentoRequestDto(dto.equipamentos(), dto.items(), dto.moedasCobre(), dto.moedasPrata(),
                dto.moedasElectro(), dto.moedasOuro(), dto.moedasPlatina());
    }
}
