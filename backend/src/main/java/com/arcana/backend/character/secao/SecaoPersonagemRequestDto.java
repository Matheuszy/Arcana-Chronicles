package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.model.TipoJogador;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SecaoPersonagemRequestDto(
        @NotBlank(message = "Nome do personagem não pode ser vazio")
        @Size(max = 100, message = "Nome do personagem não pode ter mais de 100 caracteres")
        String name,

        @NotNull(message = "Tipo do personagem não pode ser nulo")
        TipoJogador kind,

        @NotEmpty(message = "A ficha precisa de ao menos uma classe")
        @Size(max = 10, message = "A ficha pode ter no máximo 10 classes")
        List<PersonagemRequestDto.@Valid ClasseDto> classes,

        @NotBlank(message = "Raça é obrigatória")
        @Size(max = 60, message = "Raça não pode ter mais de 60 caracteres")
        String raca,

        @NotBlank(message = "Alinhamento é obrigatório")
        @Size(max = 60, message = "Alinhamento não pode ter mais de 60 caracteres")
        String alinhamento,

        @Size(max = 60, message = "Antecedente não pode ter mais de 60 caracteres")
        String antecedente,

        @Size(max = 1000, message = "Traços de personalidade não podem ter mais de 1000 caracteres")
        String tracosPersonalidade,

        @Size(max = 1000, message = "Ideais não podem ter mais de 1000 caracteres")
        String ideais,

        @Size(max = 1000, message = "Vínculos não podem ter mais de 1000 caracteres")
        String vinculos,

        @Size(max = 1000, message = "Defeitos não podem ter mais de 1000 caracteres")
        String defeitos,

        String backstory,

        String personalityPrompt,

        String avatarUrl
) {
    public static SecaoPersonagemRequestDto de(PersonagemRequestDto dto) {
        return new SecaoPersonagemRequestDto(dto.name(), dto.kind(), dto.classes(), dto.raca(), dto.alinhamento(),
                dto.antecedente(), dto.tracosPersonalidade(), dto.ideais(), dto.vinculos(), dto.defeitos(),
                dto.backstory(), dto.personalityPrompt(), dto.avatarUrl());
    }
}
