package com.arcana.backend.character;

import com.arcana.backend.character.model.Moeda;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AcrescimoDeMoedasRequestDto(
        @NotNull(message = "Escolha a moeda")
        Moeda moeda,

        @NotNull(message = "Informe quantas moedas acrescentar")
        @Min(value = 1, message = "Acrescente ao menos 1 moeda")
        @Max(value = 1000000, message = "Acrescente no máximo 1.000.000 de moedas por vez")
        Integer quantidade
) {}
