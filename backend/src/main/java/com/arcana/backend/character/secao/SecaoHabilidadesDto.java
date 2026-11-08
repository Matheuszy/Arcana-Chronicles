package com.arcana.backend.character.secao;

import com.arcana.backend.character.model.Personagem;

import java.util.List;

public record SecaoHabilidadesDto(
        String habilidades,
        String habilidadesRaca,
        String talentos,
        List<String> idiomas,
        List<String> proficienciasFerramentas,
        List<String> proficienciasArmas,
        List<String> proficienciasArmaduras
) {
    public static SecaoHabilidadesDto from(Personagem p) {
        return new SecaoHabilidadesDto(
                p.getHabilidades(),
                p.getHabilidadesRaca(),
                p.getTalentos(),
                List.copyOf(p.getIdiomas()),
                List.copyOf(p.getProficienciasFerramentas()),
                List.copyOf(p.getProficienciasArmas()),
                List.copyOf(p.getProficienciasArmaduras()));
    }
}
