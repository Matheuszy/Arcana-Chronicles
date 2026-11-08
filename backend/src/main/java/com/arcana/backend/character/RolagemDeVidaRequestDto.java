package com.arcana.backend.character;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RolagemDeVidaRequestDto(
        @NotNull(message = "Escolha o dado de vida")
        Integer lados,

        @NotNull(message = "Informe quantos dados rolar")
        @Min(value = 1, message = "Role de 1 a 20 dados")
        @Max(value = 20, message = "Role de 1 a 20 dados")
        Integer quantidade,

        @Min(value = -20, message = "O modificador deve ficar entre -20 e +20")
        @Max(value = 20, message = "O modificador deve ficar entre -20 e +20")
        Integer modificador
) {}
