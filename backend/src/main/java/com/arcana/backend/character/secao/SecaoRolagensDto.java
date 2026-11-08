package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Personagem;

import java.util.List;

public record SecaoRolagensDto(
        Long id,
        Integer hpMax,
        Integer hpCurrent,
        PersonagemResponseDto.AtributosDto attributes,
        List<PersonagemResponseDto.ClasseDto> classes,
        boolean moedasRoladas
) {
    public static SecaoRolagensDto from(Personagem p) {
        return new SecaoRolagensDto(
                p.getId(),
                p.getHpMax(),
                p.getHpCurrent(),
                PersonagemResponseDto.atributos(p),
                PersonagemResponseDto.classes(p),
                p.isMoedasRoladas());
    }
}
