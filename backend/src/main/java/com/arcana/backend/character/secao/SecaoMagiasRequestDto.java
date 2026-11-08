package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemRequestDto;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public record SecaoMagiasRequestDto(
        List<PersonagemRequestDto.@Valid SpellDto> spells,
        Map<String, Integer> spellSlots,
        Map<String, Integer> spellSlotsCurrent
) {
    public static SecaoMagiasRequestDto de(PersonagemRequestDto dto) {
        return new SecaoMagiasRequestDto(dto.spells(), dto.spellSlots(), dto.spellSlotsCurrent());
    }
}
