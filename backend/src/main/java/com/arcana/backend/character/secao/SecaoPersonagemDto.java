package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;

import java.util.List;

public record SecaoPersonagemDto(
        Long id,
        String name,
        TipoJogador kind,
        Integer level,
        List<PersonagemResponseDto.ClasseDto> classes,
        String raca,
        String alinhamento,
        String antecedente,
        String tracosPersonalidade,
        String ideais,
        String vinculos,
        String defeitos,
        String backstory,
        String personalityPrompt,
        String avatarUrl
) {
    public static SecaoPersonagemDto from(Personagem p) {
        return new SecaoPersonagemDto(
                p.getId(),
                p.getName(),
                p.getKind(),
                p.getLevel(),
                PersonagemResponseDto.classes(p),
                p.getRaca(),
                p.getAlinhamento(),
                p.getAntecedente(),
                p.getTracosPersonalidade(),
                p.getIdeais(),
                p.getVinculos(),
                p.getDefeitos(),
                p.getBackstory(),
                p.getPersonalityPrompt(),
                p.getAvatarUrl());
    }
}
