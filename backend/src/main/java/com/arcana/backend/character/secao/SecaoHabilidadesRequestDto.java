package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.PersonagemRequestDto.HabilidadeDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SecaoHabilidadesRequestDto(
        @Size(max = 30, message = "A ficha pode ter no máximo 30 habilidades de classe e subclasse")
        List<@Valid HabilidadeDto> habilidades,

        @Size(max = 30, message = "A ficha pode ter no máximo 30 habilidades de raça")
        List<@Valid HabilidadeDto> habilidadesRaca,

        @Size(max = 30, message = "A ficha pode ter no máximo 30 talentos")
        List<@Valid HabilidadeDto> talentos,

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
