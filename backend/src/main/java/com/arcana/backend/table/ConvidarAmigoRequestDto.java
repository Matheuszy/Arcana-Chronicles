package com.arcana.backend.table;

import jakarta.validation.constraints.NotNull;

public record ConvidarAmigoRequestDto(

        @NotNull(message = "Escolha o amigo que você quer convidar")
        Long convidadoId
) {}
