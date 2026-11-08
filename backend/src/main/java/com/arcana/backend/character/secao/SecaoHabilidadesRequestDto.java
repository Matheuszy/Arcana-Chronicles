package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemRequestDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SecaoHabilidadesRequestDto(
        @Size(max = 5000, message = "Habilidades não podem ter mais de 5000 caracteres")
        String habilidades,

        @Size(max = 5000, message = "Habilidades de raça não podem ter mais de 5000 caracteres")
        String habilidadesRaca,

        @Size(max = 5000, message = "Talentos não podem ter mais de 5000 caracteres")
        String talentos,

        @Size(max = 30, message = "A ficha pode ter no máximo 30 idiomas")
        List<@NotBlank(message = "Idioma não pode ser vazio")
             @Size(max = 60, message = "Idioma não pode ter mais de 60 caracteres") String> idiomas,

        @Size(max = 30, message = "A ficha pode ter no máximo 30 proficiências em ferramentas")
        List<@NotBlank(message = "Proficiência em ferramenta não pode ser vazia")
             @Size(max = 60, message = "Proficiência em ferramenta não pode ter mais de 60 caracteres") String> proficienciasFerramentas,

        @Size(max = 30, message = "A ficha pode ter no máximo 30 proficiências em armas")
        List<@NotBlank(message = "Proficiência em arma não pode ser vazia")
             @Size(max = 60, message = "Proficiência em arma não pode ter mais de 60 caracteres") String> proficienciasArmas,

        @Size(max = 30, message = "A ficha pode ter no máximo 30 proficiências em armaduras")
        List<@NotBlank(message = "Proficiência em armadura não pode ser vazia")
             @Size(max = 60, message = "Proficiência em armadura não pode ter mais de 60 caracteres") String> proficienciasArmaduras
) {
    public static SecaoHabilidadesRequestDto de(PersonagemRequestDto dto) {
        return new SecaoHabilidadesRequestDto(dto.habilidades(), dto.habilidadesRaca(), dto.talentos(), dto.idiomas(),
                dto.proficienciasFerramentas(), dto.proficienciasArmas(), dto.proficienciasArmaduras());
    }
}
