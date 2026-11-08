package com.arcana.backend.character;

import com.arcana.backend.character.model.Moeda;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RolagemDeMoedasRequestDto(
        @NotNull(message = "Escolha a moeda")
        Moeda moeda,

        @NotNull(message = "Escolha o dado")
        Integer lados,

        @NotNull(message = "Informe quantos dados rolar")
        @Min(value = 1, message = "Role de 1 a 20 dados")
        @Max(value = 20, message = "Role de 1 a 20 dados")
        Integer quantidade,

        @Min(value = -20, message = "O modificador deve ficar entre -20 e +20")
        @Max(value = 20, message = "O modificador deve ficar entre -20 e +20")
        Integer modificador,

        @Min(value = 1, message = "O multiplicador deve ficar entre 1 e 1000")
        @Max(value = 1000, message = "O multiplicador deve ficar entre 1 e 1000")
        Integer multiplicador
) {}
