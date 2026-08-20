package com.arcana.backend.table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MesaRequestDto(

        @NotBlank(message = "Nome da mesa não pode ser vazio")
        @Size(max = 255)
        String name,

        String description,

        /** Nome do mestre para exibição */
        @NotBlank(message = "Nome do mestre não pode ser vazio")
        String masterName
) {}
