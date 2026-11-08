package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Personagem;

import java.util.List;
import java.util.Map;

public record SecaoMagiasDto(
        List<PersonagemResponseDto.SpellDto> spells,
        Map<String, Integer> spellSlots,
        Map<String, Integer> spellSlotsCurrent
) {
    public static SecaoMagiasDto from(Personagem p) {
        return new SecaoMagiasDto(
                PersonagemResponseDto.magias(p),
                PersonagemResponseDto.espacosDeMagia(p),
                PersonagemResponseDto.espacosDeMagiaRestantes(p));
    }
}
