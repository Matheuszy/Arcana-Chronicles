package com.arcana.backend.character;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AplicarPacoteRequestDto(
        @NotBlank(message = "Escolha um pacote de equipamento")
        @Size(max = 30, message = "Pacote de equipamento inválido")
        String pacote
) {}
