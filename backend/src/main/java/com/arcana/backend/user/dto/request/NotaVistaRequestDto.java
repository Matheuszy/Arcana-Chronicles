package com.arcana.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NotaVistaRequestDto(
        @NotBlank(message = "Informe a versão das notas vistas")
        @Pattern(regexp = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", message = "A versão deve ser como 1.2.0")
        String versao
) {}
