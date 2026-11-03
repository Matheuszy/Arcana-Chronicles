package com.arcana.backend.friend.dto.request;

import jakarta.validation.constraints.NotNull;

public record EnviarSolicitacaoAmizadeRequestDto(

        @NotNull(message = "Escolha para quem enviar o pedido de amizade")
        Long destinatarioId
) {}
