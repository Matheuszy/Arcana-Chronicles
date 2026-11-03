package com.arcana.backend.friend.dto.request;

import jakarta.validation.constraints.NotNull;

public record BloquearUsuarioRequestDto(

        @NotNull(message = "Escolha quem você quer bloquear")
        Long usuarioId
) {}
