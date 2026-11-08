package com.arcana.backend.character;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GastoDeDadosDeVidaRequestDto(
        @NotNull(message = "Escolha a classe dos dados de vida")
        @Min(value = 0, message = "Classe inválida")
        Integer classe,

        @NotNull(message = "Informe quantos dados de vida gastar")
        @Min(value = 1, message = "Gaste de 1 a 20 dados de vida")
        @Max(value = 20, message = "Gaste de 1 a 20 dados de vida")
        Integer quantidade,

        @Min(value = -20, message = "O modificador deve ficar entre -20 e +20")
        @Max(value = 20, message = "O modificador deve ficar entre -20 e +20")
        Integer modificador
) {}
