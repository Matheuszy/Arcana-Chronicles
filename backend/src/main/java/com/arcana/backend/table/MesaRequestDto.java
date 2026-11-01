package com.arcana.backend.table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MesaRequestDto(

        @NotBlank(message = "Nome da mesa não pode ser vazio")
        @Size(max = 255, message = "Nome da mesa não pode ter mais de 255 caracteres")
        String name,

        String description,

        @Size(max = 50, message = "A mesa pode começar com no máximo 50 NPCs vinculados")
        List<@NotNull(message = "NPC da mesa não informado") Long> npcIds
) {

    public MesaRequestDto(String name, String description) {
        this(name, description, null);
    }
}
