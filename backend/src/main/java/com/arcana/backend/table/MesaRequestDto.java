package com.arcana.backend.table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MesaRequestDto(

        @NotBlank(message = "Nome da mesa não pode ser vazio")
        @Size(max = 255, message = "Nome da mesa não pode ter mais de 255 caracteres")
        String name,

        String description
) {}
