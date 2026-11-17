package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Personagem;

import java.util.List;

public record SecaoHabilidadesDto(
        List<PersonagemResponseDto.HabilidadeDto> habilidades,
        List<PersonagemResponseDto.HabilidadeDto> habilidadesRaca,
        List<PersonagemResponseDto.HabilidadeDto> talentos,
        List<String> idiomas,
        List<String> proficienciasFerramentas,
        List<String> proficienciasArmas,
        List<String> proficienciasArmaduras
) {
    public static SecaoHabilidadesDto from(Personagem p) {
        return new SecaoHabilidadesDto(
                PersonagemResponseDto.habilidades(p.getHabilidades()),
                PersonagemResponseDto.habilidades(p.getHabilidadesRaca()),
                PersonagemResponseDto.habilidades(p.getTalentos()),
                List.copyOf(p.getIdiomas()),
                List.copyOf(p.getProficienciasFerramentas()),
                List.copyOf(p.getProficienciasArmas()),
                List.copyOf(p.getProficienciasArmaduras()));
    }
}
