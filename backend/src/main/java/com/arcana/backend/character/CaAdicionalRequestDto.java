package com.arcana.backend.character;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CaAdicionalRequestDto(
        @NotNull(message = "Informe a CA adicional")
        @Min(value = 0, message = "A CA adicional não pode ser negativa")
        @Max(value = 20, message = "A CA adicional não pode passar de 20")
        Integer valor
) {}
